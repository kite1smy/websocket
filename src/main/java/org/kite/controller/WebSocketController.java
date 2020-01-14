package org.kite.controller;

import io.reactivex.subjects.BehaviorSubject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author kite
 * @date 2020/01/01
 */
@ServerEndpoint("/websocket/{id}/{name}")
@RestController
@Slf4j
public class WebSocketController {

    /**
     * 用来记录当前连接数的变量
     */
    private static CountObservable onlineCount = new CountObservable();


    /**
     * 保存 组id->组成员 的映射关系
     */
    private static ConcurrentHashMap<String, List<Session>> groupMemberInfoMap = new ConcurrentHashMap<>();


    private static void broadcast(String id, String msg) {
        List<Session> sessionList = groupMemberInfoMap.get(id);
        sessionList.forEach(session -> {
            try {
                Optional.of(session)
                        .map(Session::getBasicRemote)
                        .get()
                        .sendText(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("id") String id, @PathParam("name") String name) throws Exception {


        List<Session> sessionList = groupMemberInfoMap.get(id);
        if (sessionList == null) {
            synchronized (WebSocketController.class) {
                if (groupMemberInfoMap.get(id) == null) {
                    sessionList = new ArrayList<>();
                    groupMemberInfoMap.put(id, sessionList);
                    onlineCount
                            .getObservable()
                            .subscribe((count) -> broadcast(id, "在线人数: " + count));
                }

            }
        }
        sessionList.add(session);

        log.info("Connection connected  sid: {}, sessionList size: {}", id, sessionList.size());

        onlineCount.incrementAndGet();

    }

    @OnClose
    public void onClose(Session session, @PathParam("id") String id) {

        List<Session> sessionList = groupMemberInfoMap.get(id);

        log.info("Connection closed sid: {}, sessionList size: {}", id, sessionList.size());

        sessionList.remove(session);
        if (sessionList.size() == 0) {
            synchronized (WebSocketController.class) {
                if (sessionList.size() == 0) {
                    groupMemberInfoMap.remove(id);

                }
            }
        }

        onlineCount.decrementAndGet();

    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("id") String id, @PathParam("name") String name) throws IOException {
        List<Session> sessionList = groupMemberInfoMap.get(id);
        broadcast(id, name + " : " + message);
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
