package org.kite;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.kite.websocket.MsgHandler;

/**
 * 服务端
 */
public class SocketServer {

    public static void main(String[] args) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();

        b.group(bossGroup, workerGroup);
        b.channel(NioServerSocketChannel.class);  // 服务器信道的处理方式

        b.childHandler(new ChannelInitializer<SocketChannel>() { // 客户端信道的处理器方式
            @Override
            protected void initChannel(SocketChannel ch){
                ch.pipeline().addLast(
                        new HttpServerCodec(), // Http 服务器编解码器
                        new HttpObjectAggregator(65535), // 内容长度限制
                        new WebSocketServerProtocolHandler("/"), // WebSocket 协议处理器, 在这里处理握手、ping、pong 等消息
                        new MsgHandler() // 自定义的消息处理器


                );
            }

        });


        try {
            // 绑定 12345 端口,
            ChannelFuture f = b.bind(12345).sync();

            if (f.isSuccess()) {
                System.out.println("服务器启动成功");
            }

            // 等待服务器信道关闭,
            // 也就是不要退出应用程序,
            // 让应用程序可以一直提供服务
            f.channel().closeFuture().sync();
        } catch (Exception ex) {
            ex.printStackTrace();
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
