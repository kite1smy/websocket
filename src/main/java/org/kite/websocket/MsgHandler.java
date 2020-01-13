package org.kite.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;


public class MsgHandler extends SimpleChannelInboundHandler<Object> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelActive    ");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("收到客户端消息, msg = " + msg);

        // WebSocket 二进制消息会通过 HttpServerCodec 解码成 BinaryWebSocketFrame 类对象
        TextWebSocketFrame frame = (TextWebSocketFrame) msg;
        ByteBuf byteBuf = frame.content();

        // 拿到真实的字节数组并打印
        byte[] byteArray = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(byteArray);

        System.out.println("收到的字节 = ");

//        for (byte b : byteArray) {
//            System.out.print(b);
//            System.out.print(", ");
//        }


        String myStr = new String(byteArray);

        System.out.println(myStr);
    }

    /**
     * 服务端
     */
    public static class SocketServer {

        public static void main(String[] args) {
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();

            ServerBootstrap b = new ServerBootstrap();

            b.group(bossGroup, workerGroup);
            b.channel(NioServerSocketChannel.class);  // 服务器信道的处理方式

            b.childHandler(new ChannelInitializer<SocketChannel>() { // 客户端信道的处理器方式
                @Override
                protected void initChannel(SocketChannel ch) {
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
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }
    }
}
