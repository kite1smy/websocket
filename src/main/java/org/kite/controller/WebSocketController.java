package org.kite.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    private static AtomicInteger onlineCount = new AtomicInteger();

    /**
     * 保存 组id->组成员 的映射关系
     */
    private static ConcurrentHashMap<String, List<Session>> groupMemberInfoMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("id") String id, @PathParam("name") String name) throws Exception {
        List<Session> sessionList = groupMemberInfoMap.computeIfAbsent(id, k -> new ArrayList<>());
        sessionList.add(session);

        log.info("Connection connected");
        log.info("sid: {}, sessionList size: {}", id, sessionList.size());

        onlineCount.incrementAndGet();
    }

    @OnClose
    public void onClose(Session session, @PathParam("id") String id) {

        List<Session> sessionList = groupMemberInfoMap.get(id);
        if (sessionList == null) {
            log.warn("没有参数");
            return;
        }
        sessionList.remove(session);
        log.info("Connection closed");
        log.info("sid: {}, sessionList size: {}", id, sessionList.size());

        onlineCount.decrementAndGet();
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("id") String id, @PathParam("name") String name) throws IOException {
        List<Session> sessionList = groupMemberInfoMap.get(id);
        // 先一个群组内的成员发送消息
        sessionList.forEach(item -> {
            try {
                String text = name + ": " + message;
                item.getBasicRemote().sendText(text);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("Error while websocket. ", error);
    }

}
