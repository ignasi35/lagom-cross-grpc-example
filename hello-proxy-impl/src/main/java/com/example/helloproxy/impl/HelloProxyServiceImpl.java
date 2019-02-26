package com.example.helloproxy.impl;

import akka.NotUsed;
import com.example.helloproxy.api.HelloProxyService;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import example.myapp.helloworld.grpc.GreeterServiceClient;
import example.myapp.helloworld.grpc.HelloReply;
import example.myapp.helloworld.grpc.HelloRequest;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HelloProxyServiceImpl implements HelloProxyService {

    private GreeterServiceClient greeterClient;

    @Inject
    public HelloProxyServiceImpl(
        GreeterServiceClient greeterClient) {
        this.greeterClient = greeterClient;
    }

    @Override
    public ServiceCall<NotUsed, String> proxyViaGrpc(String id) {
        return req -> greeterClient
            .sayHello(
                HelloRequest
                    .newBuilder()
                    .setName(id)
                    .build()
            ).thenApply(
                HelloReply::getMessage
            );
    }

}
