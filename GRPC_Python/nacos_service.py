# nacos_service.py
# Nacos服务注册与管理模块

import os
import time
import asyncio
import threading
import logging
from typing import Optional, Dict, Any
from dataclasses import dataclass

# 从 v2.nacos 库中导入所有需要的类
from v2.nacos import (
    NacosNamingService,
    ClientConfigBuilder,
    GRPCConfig,
    RegisterInstanceParam,
    DeregisterInstanceParam,
    Instance
)

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

@dataclass
class NacosConfig:
    """Nacos配置数据类"""
    server_ip: str
    port: int
    namespace: str
    username: str
    password: str
    group_name: str
    enable_auth: bool = True

@dataclass
class ServiceInfo:
    """服务信息数据类"""
    name: str
    ip: str
    port: int
    group: str
    cluster: str = 'DEFAULT'
    ephemeral: bool = True
    metadata: Optional[Dict[str, str]] = None

class NacosServiceManager:
    """Nacos服务管理器"""

    def __init__(self, nacos_config: NacosConfig, service_info: ServiceInfo):
        self.nacos_config = nacos_config
        self.service_info = service_info
        self.naming_client: Optional[NacosNamingService] = None
        self.is_registered = False
        self.is_running = False
        self.registration_task: Optional[threading.Thread] = None
        self._lock = threading.Lock()
        self._loop: Optional[asyncio.AbstractEventLoop] = None

        logger.info(f"初始化Nacos服务管理器: {service_info.name}")

    async def _create_client(self) -> bool:
        """创建Nacos命名服务客户端"""
        try:
            server_address = f"{self.nacos_config.server_ip}:{self.nacos_config.port}"

            # 构建客户端配置
            builder = (
                ClientConfigBuilder()
                .server_address(server_address)
                .namespace_id(self.nacos_config.namespace)
                .log_level('INFO')
                .grpc_config(GRPCConfig(grpc_timeout=5000))
            )
            
            # 只有在启用认证时才设置用户名和密码
            if self.nacos_config.enable_auth and self.nacos_config.username and self.nacos_config.password:
                builder = builder.username(self.nacos_config.username).password(self.nacos_config.password)
            
            client_config = builder.build()

            self.naming_client = await NacosNamingService.create_naming_service(client_config)
            logger.info(f"✅ Nacos客户端创建成功: {server_address}")
            return True

        except Exception as e:
            logger.error(f"❌ 创建Nacos客户端失败: {e}")
            self.naming_client = None
            return False

    async def register_service(self) -> bool:
        """注册服务到Nacos"""
        if self.is_registered:
            logger.info("服务已经注册，跳过重复注册")
            return True

        try:
            # 确保客户端存在
            if not self.naming_client:
                if not await self._create_client():
                    return False

            # 准备元数据
            metadata = {}
            if hasattr(self.service_info, 'metadata') and self.service_info.metadata:
                metadata = self.service_info.metadata
            
            register_param = RegisterInstanceParam(
                service_name=self.service_info.name,
                group_name=self.service_info.group,
                ip=self.service_info.ip,
                port=self.service_info.port,
                cluster_name=self.service_info.cluster,
                ephemeral=self.service_info.ephemeral,
                metadata=metadata
            )

            response = await self.naming_client.register_instance(request=register_param)

            if response:
                self.is_registered = True
                logger.info(f"✅ 服务 '{self.service_info.name}' 注册成功！")
                logger.info(f"服务地址: http://{self.service_info.ip}:{self.service_info.port}")
                logger.info(f"健康检查地址: http://{self.service_info.ip}:{self.service_info.port}/health")
                return True
            else:
                logger.error(f"❌ 服务 '{self.service_info.name}' 注册失败")
                return False

        except Exception as e:
            logger.error(f"注册服务时出错: {e}")
            return False

    async def deregister_service(self) -> bool:
        """从Nacos注销服务"""
        if not self.is_registered or not self.naming_client:
            logger.info("服务未注册或客户端不存在，跳过注销")
            return True

        try:
            deregister_param = DeregisterInstanceParam(
                service_name=self.service_info.name,
                group_name=self.service_info.group,
                ip=self.service_info.ip,
                port=self.service_info.port,
                cluster_name=self.service_info.cluster,
                ephemeral=self.service_info.ephemeral
            )

            # 使用超时机制
            try:
                await asyncio.wait_for(
                    self.naming_client.deregister_instance(request=deregister_param),
                    timeout=5.0
                )
                logger.info("✅ 服务已成功注销")
                self.is_registered = False
                return True
            except asyncio.TimeoutError:
                logger.warning("⚠️  注销服务超时，但可能已成功")
                self.is_registered = False
                return True
            except Exception as e:
                logger.error(f"❌ 注销服务失败: {e}")
                return False

        except Exception as e:
            logger.error(f"注销服务时出错: {e}")
            return False

    async def shutdown_client(self) -> bool:
        """关闭Nacos客户端"""
        if not self.naming_client:
            return True

        try:
            if hasattr(self.naming_client, 'shutdown'):
                await asyncio.wait_for(
                    self.naming_client.shutdown(),
                    timeout=3.0
                )
                logger.info("✅ Nacos客户端已关闭")
            else:
                logger.info("ℹ️  客户端没有shutdown方法，跳过关闭")

            self.naming_client = None
            return True

        except asyncio.TimeoutError:
            logger.warning("⚠️  关闭客户端超时")
            return False
        except Exception as e:
            logger.error(f"⚠️  关闭客户端时出错: {e}")
            return False

    def start_registration(self) -> bool:
        """在后台线程中启动服务注册"""
        if self.registration_task and self.registration_task.is_alive():
            logger.warning("注册任务已在运行")
            return False

        def run_registration_async():
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            # 保存事件循环引用，便于跨线程停止
            self._loop = loop

            try:
                # 注册服务
                success = loop.run_until_complete(self.register_service())
                if success:
                    logger.info("✅ 服务注册成功，使用loop.run_forever()保持连接")
                    logger.info("💡 客户端将自动维护心跳，无需手动发送")

                    # 保持连接运行
                    while self.is_running:
                        try:
                            loop.run_forever()
                        except Exception as e:
                            logger.error(f"Loop运行出错: {e}")
                            if self.is_running:
                                logger.info("尝试重新连接...")
                                time.sleep(5)
                                try:
                                    loop.run_until_complete(self.register_service())
                                except Exception as reconnect_error:
                                    logger.error(f"重连失败: {reconnect_error}")
                            else:
                                break
                else:
                    logger.error("❌ 服务注册失败")

            except KeyboardInterrupt:
                logger.info("收到退出信号，正在清理...")
            finally:
                try:
                    loop.run_until_complete(self.deregister_service())
                    loop.run_until_complete(self.shutdown_client())
                except Exception as e:
                    logger.error(f"清理时出错: {e}")
                finally:
                    self._loop = None
                    loop.close()
                    logger.info("✅ Nacos注册线程已完全退出")

        self.is_running = True
        self.registration_task = threading.Thread(target=run_registration_async, daemon=True)
        self.registration_task.start()

        logger.info("✅ Nacos服务注册已启动")
        return True

    def stop_registration(self) -> bool:
        """停止服务注册"""
        with self._lock:
            if not self.is_running:
                return True

            self.is_running = False
            logger.info("正在停止Nacos服务注册...")

            # 请求事件循环退出，以便触发注销与清理
            if self._loop and self._loop.is_running():
                try:
                    self._loop.call_soon_threadsafe(self._loop.stop)
                except Exception as e:
                    logger.warning(f"停止事件循环时出错: {e}")

            # 等待注册任务完成
            if self.registration_task and self.registration_task.is_alive():
                self.registration_task.join(timeout=5)
                if self.registration_task.is_alive():
                    logger.warning("⚠️  Nacos线程清理超时")
                else:
                    logger.info("✅ Nacos线程已完全退出")

            return True

    def get_status(self) -> Dict[str, Any]:
        """获取服务状态信息"""
        return {
            'is_registered': self.is_registered,
            'is_running': self.is_running,
            'client_exists': self.naming_client is not None,
            'thread_alive': self.registration_task.is_alive() if self.registration_task else False,
            'service_info': {
                'name': self.service_info.name,
                'ip': self.service_info.ip,
                'port': self.service_info.port,
                'group': self.service_info.group
            }
        }

    async def health_check(self) -> bool:
        """健康检查"""
        try:
            if not self.naming_client:
                return False

            # 这里可以添加更详细的健康检查逻辑
            # 比如检查与Nacos服务器的连接状态
            return self.is_registered

        except Exception as e:
            logger.error(f"健康检查失败: {e}")
            return False

# 便捷函数
def create_nacos_manager_from_config(nacos_config_dict: Dict[str, Any],
                                   service_name: str,
                                   service_ip: str,
                                   service_port: int) -> NacosServiceManager:
    """从配置字典创建Nacos服务管理器"""
    nacos_config = NacosConfig(
        server_ip=nacos_config_dict['nacos_server_ip'],
        port=nacos_config_dict['nacos_port'],
        namespace=nacos_config_dict['nacos_namespace'],
        username=nacos_config_dict['nacos_user'],
        password=nacos_config_dict['nacos_password'],
        group_name=nacos_config_dict['nacos_groupName']
    )

    service_info = ServiceInfo(
        name=service_name,
        ip=service_ip,
        port=service_port,
        group=nacos_config_dict['nacos_groupName'],
        metadata=nacos_config_dict.get('metadata', {})
    )

    return NacosServiceManager(nacos_config, service_info)
