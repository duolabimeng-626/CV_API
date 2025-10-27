#!/usr/bin/env python3
# main.py
# YOLO gRPC服务主启动文件

import os
import sys
import argparse
import logging
from pathlib import Path

# 添加项目根目录到Python路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

# 导入服务模块和配置管理器
from models.yolo.grpc_yolo_server import serve
from config import (get_default_port, get_default_weights, get_default_model_type, 
                   get_default_model_name, get_yolo_instances, get_yolo_instance, 
                   get_default_instance_id)

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler('yolo_service.log')
    ]
)
logger = logging.getLogger(__name__)

def parse_arguments():
    """解析命令行参数"""
    parser = argparse.ArgumentParser(description='YOLO gRPC服务启动器')
    
    parser.add_argument(
        '--weights', '-w',
        type=str,
        default=os.getenv('YOLO_WEIGHTS', get_default_weights()),
        help=f'YOLO模型权重文件路径 (默认: {get_default_weights()})'
    )
    
    parser.add_argument(
        '--port', '-p',
        type=int,
        default=int(os.getenv('GRPC_PORT', str(get_default_port()))),
        help=f'gRPC服务端口 (默认: {get_default_port()})'
    )
    
    parser.add_argument(
        '--model-type',
        type=str,
        choices=['detection', 'segmentation'],
        default=os.getenv('MODEL_TYPE', get_default_model_type()),
        help=f'模型类型 (默认: {get_default_model_type()})'
    )
    
    parser.add_argument(
        '--model-name',
        type=str,
        default=os.getenv('MODEL_NAME', get_default_model_name()),
        help=f'模型名称 (默认: {get_default_model_name()})'
    )
    
    parser.add_argument(
        '--instance',
        type=str,
        default=os.getenv('YOLO_INSTANCE', get_default_instance_id()),
        help=f'YOLO实例ID (默认: {get_default_instance_id()})'
    )
    
    parser.add_argument(
        '--list-instances',
        action='store_true',
        help='列出所有可用的YOLO实例'
    )
    
    parser.add_argument(
        '--no-nacos',
        action='store_true',
        help='禁用Nacos注册'
    )
    
    parser.add_argument(
        '--nacos-server',
        type=str,
        default=os.getenv('NACOS_SERVER', '127.0.0.1:8848'),
        help='Nacos服务器地址 (默认: 127.0.0.1:8848)'
    )
    
    parser.add_argument(
        '--nacos-namespace',
        type=str,
        default=os.getenv('NACOS_NAMESPACE', 'public'),
        help='Nacos命名空间 (默认: public)'
    )
    
    return parser.parse_args()

def validate_weights(weights_path: str) -> bool:
    """验证模型权重文件是否存在"""
    if not os.path.exists(weights_path):
        logger.error(f"❌ 模型权重文件不存在: {weights_path}")
        return False
    
    if not weights_path.endswith(('.pt', '.onnx')):
        logger.warning(f"⚠️  权重文件格式可能不支持: {weights_path}")
    
    logger.info(f"✅ 模型权重文件验证通过: {weights_path}")
    return True

def setup_environment():
    """设置环境变量"""
    # 设置CUDA相关环境变量
    if os.getenv('CUDA_VISIBLE_DEVICES') is None:
        os.environ['CUDA_VISIBLE_DEVICES'] = '0'
    
    # 设置PyTorch相关环境变量
    os.environ['TORCH_HOME'] = str(project_root / 'models' / 'torch_cache')
    
    logger.info("✅ 环境变量设置完成")

def list_instances():
    """列出所有可用的YOLO实例"""
    instances = get_yolo_instances()
    print("\n📋 可用的YOLO实例:")
    print("=" * 60)
    for instance_id, instance in instances.items():
        print(f"ID: {instance_id}")
        print(f"  名称: {instance.name}")
        print(f"  端口: {instance.port}")
        print(f"  模型: {instance.model_config.model_name} ({instance.model_config.model_type})")
        print(f"  权重: {instance.model_config.weights}")
        print(f"  描述: {instance.metadata.get('description', 'N/A')}")
        print("-" * 40)
    print(f"\n默认实例: {get_default_instance_id()}")
    print("使用方法: python main.py --instance <instance_id>")

def main():
    """主函数"""
    try:
        # 解析命令行参数
        args = parse_arguments()
        
        # 如果请求列出实例，则显示并退出
        if args.list_instances:
            list_instances()
            return
        
        logger.info("🚀 启动YOLO gRPC服务...")
        logger.info(f"参数: {vars(args)}")
        
        # 设置环境
        setup_environment()
        
        # 获取实例配置
        try:
            instance_config = get_yolo_instance(args.instance)
            logger.info(f"✅ 使用实例: {args.instance}")
        except ValueError as e:
            logger.error(f"❌ {e}")
            logger.info("使用 --list-instances 查看可用实例")
            sys.exit(1)
        
        # 验证权重文件
        weights_path = args.weights if args.weights != get_default_weights() else instance_config.model_config.weights
        if not validate_weights(weights_path):
            sys.exit(1)
        
        # 设置Nacos相关环境变量
        if not args.no_nacos:
            nacos_server = args.nacos_server.split(':')
            if len(nacos_server) == 2:
                os.environ['NACOS_SERVER_IP'] = nacos_server[0]
                os.environ['NACOS_SERVER_PORT'] = nacos_server[1]
            os.environ['NACOS_NAMESPACE'] = args.nacos_namespace
        
        # 启动服务
        logger.info("=" * 50)
        logger.info("🎯 YOLO gRPC服务配置:")
        logger.info(f"   实例ID: {args.instance}")
        logger.info(f"   实例名称: {instance_config.name}")
        logger.info(f"   模型权重: {weights_path}")
        logger.info(f"   服务端口: {args.port}")
        logger.info(f"   模型类型: {instance_config.model_config.model_type}")
        logger.info(f"   模型名称: {instance_config.model_config.model_name}")
        logger.info(f"   Nacos注册: {'禁用' if args.no_nacos else '启用'}")
        if not args.no_nacos:
            logger.info(f"   Nacos服务器: {args.nacos_server}")
            logger.info(f"   Nacos命名空间: {args.nacos_namespace}")
        logger.info("=" * 50)
        
        # 启动gRPC服务
        serve(
            weights=weights_path,
            port=args.port,
            model_type=instance_config.model_config.model_type,
            model_name=instance_config.model_config.model_name,
            enable_nacos=not args.no_nacos,
            instance_id=args.instance
        )
        
    except KeyboardInterrupt:
        logger.info("👋 收到退出信号，服务已停止")
    except Exception as e:
        logger.error(f"❌ 服务启动失败: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
