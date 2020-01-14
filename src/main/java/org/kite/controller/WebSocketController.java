package org.kite.controller;

import io.reactivex.subjects.BehaviorSubject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author kite
 * @date 2020/01/01
 */
@ServerEndpoint("/websocket/{name}")
@RestController
@Slf4j
public class WebSocketController {

    /**
     * 用来记录当前连接数的变量
     */
    private static CountObservable onlineCount = new CountObservable();

    /**
     * 开始订阅
     */
    static {
        onlineCount
                .getObservable()
                .subscribe((counter) -> broadcast("在线人数: " + counter));
    }

    /**
     * 保存 用户名 -> session
     */
    private static ConcurrentHashMap<String, Session> contextHolder = new ConcurrentHashMap<>();

    private static void broadcast(String msg) {
        contextHolder
                .values()
                .forEach(session -> {
                    try {
                        if (session.isOpen()) {
                            Optional
                                    .of(session)
                                    .map(Session::getBasicRemote)
                                    .get()
                                    .sendText(msg);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("name") String name) throws Exception {

        log.info("Connection connected  name: {}", name);

        contextHolder.put(name, session);

        onlineCount.incrementAndGet();
    }

    @OnClose
    public void onClose(Session session, @PathParam("name") String name) {

        log.info("Connection disconnected  name: {}", name);

        contextHolder.remove(name, session);

        onlineCount.decrementAndGet();

    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("id") String id, @PathParam("name") String name) throws IOException {
        broadcast(name + " : " + message);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("Error while websocket. ", error);
    }

    /**
     * 定义一个可被观察的计数器
     * 当计数器的值发生变化的时候,向他的订阅者们发送事件
     */
    private static class CountObservable {
        /**
         * 计数器
         */
        private static AtomicInteger onlineCount = new AtomicInteger();

        /**
         * 可被观察对象
         */
        private BehaviorSubject<Integer> subject = BehaviorSubject.create();


        void incrementAndGet() {
            subject.onNext(
                    onlineCount.incrementAndGet()
            );
        }

        void decrementAndGet() {
            subject.onNext(
                    onlineCount.decrementAndGet()
            );
        }

        BehaviorSubject<Integer> getObservable() {
            return this.subject;
        }
    }

}
