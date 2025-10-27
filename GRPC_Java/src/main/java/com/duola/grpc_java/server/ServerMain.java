package com.duola.grpc_java.server;

import com.duola.grpc_java.server.imp.InferenceStreamServiceImpl;
import io.grpc.*;


public class ServerMain {

    public static void main(String[] args) throws Exception {
        int port = 50051;
        Server server = ServerBuilder.forPort(port)
                .addService(new InferenceStreamServiceImpl())
                .build()
                .start();

        System.out.println("gRPC server started on port " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down gRPC server...");
            server.shutdown();
            System.out.println("Server stopped.");
        }));

        server.awaitTermination();
    }
}

//package com.duola.grpc_java.server;
//
//
//import io.grpc.Server;
//import io.grpc.ServerBuilder;
//
//
//public class ServerMain {
//
//    public static void main(String[] args) throws Exception {
//        int port = 50051;
//        Server server = ServerBuilder.forPort(port)
//                .addService(new InferenceStreamServiceImpl())
//                .build()
//                .start();
//
//        System.out.println("🚀 gRPC server started on port " + port);
//
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            System.out.println("🛑 Shutting down gRPC server...");
//            server.shutdown();
//            System.out.println("✅ Server stopped.");
//        }));
//
//        server.awaitTermination();
//    }
//}
//
