package org.kite.controller;

import io.reactivex.disposables.Disposable;
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
    private static OnlineCount onlineCount = new OnlineCount();

    /**
     * 保存 组id->组成员 的映射关系
     */
    private static ConcurrentHashMap<String, List<Session>> groupMemberInfoMap = new ConcurrentHashMap<>();


    private void broadcast(String id, String msg) {
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
        List<Session> sessionList = groupMemberInfoMap.computeIfAbsent(id, k -> new ArrayList<>());
        sessionList.add(session);

        log.info("Connection connected  sid: {}, sessionList size: {}", id, sessionList.size());

        onlineCount.incrementAndGet();
        onlineCount.subscribe(session);
    }

    @OnClose
    public void onClose(Session session, @PathParam("id") String id) {

        List<Session> sessionList = groupMemberInfoMap.get(id);

        log.info("Connection closed sid: {}, sessionList size: {}", id, sessionList.size());

        sessionList.remove(session);
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

    private static class OnlineCount {
        private static AtomicInteger onlineCount = new AtomicInteger();

        private BehaviorSubject<Integer> subject = BehaviorSubject.create();

        void subscribe(Session session) {

            Disposable dispose;
            dispose = subject.subscribe(integer -> {
                if (session.isOpen()) {
                    Optional.of(session)
                            .map(Session::getBasicRemote)
                            .get()
                            .sendText("在线人数: " + integer);
                } else {
//                    dispose.dispose();
                }
            });


        }

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
    }

}
