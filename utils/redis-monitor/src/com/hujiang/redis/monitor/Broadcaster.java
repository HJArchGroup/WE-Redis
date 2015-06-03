package com.hujiang.redis.monitor;

import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.hujiang.redis.monitor.RedisClusterInfo;

@ServerEndpoint(value = "/info")
public class Broadcaster {
	//private static final Configuration conf = ;
	private static final RedisClusterInfo info = new RedisClusterInfo(Configuration.getInstance().getRedisServers());
	
	/*
	private static Configuration conf = new Configuration();
	public static RedisClusterInfo info = new RedisClusterInfo(conf.getRedisHost(), conf.getRedisPort());
	*/
	

	@OnOpen
	public void OnOpen(Session session, EndpointConfig config) {  
		;
	}
	
	@OnMessage
	public void OnMessage(Session session, String message) {
		if (message.contains("all")) {
			Broadcaster.info.update();
			session.getAsyncRemote().sendText(Broadcaster.info.toJSON());
		}
	}
	
	@OnClose
	public void OnClose(Session session) {
		;
	}
}
