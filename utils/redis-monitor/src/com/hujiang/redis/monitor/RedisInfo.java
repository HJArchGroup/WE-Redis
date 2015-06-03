package com.hujiang.redis.monitor;

import redis.clients.jedis.Jedis;

import com.hujiang.redis.monitor.info.CPUInfo;
//import com.hujiang.redis.monitor.info.ClusterInfo;
import com.hujiang.redis.monitor.info.MemoryInfo;
import com.hujiang.redis.monitor.info.StatsInfo;
import com.hujiang.redis.monitor.info.SystemInfo;

public class RedisInfo {
	private CPUInfo		cpu		= new CPUInfo();
	private MemoryInfo	memory	= new MemoryInfo();
	private SystemInfo	system	= new SystemInfo();
	private StatsInfo	stats	= new StatsInfo();
	
	private Jedis		client	= null;
	private String		json	= null;
	
	public RedisInfo(final String host, int port) {
		this.client = new Jedis(host, port);
	}
	
	public void close() {
		this.client.close();
	}
	
	public void clear() {
		if (this.cpu != null) {
			this.cpu.clear();
		}
		if (this.memory != null) {
			this.memory.clear();
		}
		if (this.system != null) {
			this.memory.clear();
		}
		if (this.stats != null) {
			this.stats.clear();
		}
		
		this.json = "";
	}
	
	public void update() {
		this.clear();
		if (!this.client.isConnected()) {
			this.client.connect();
		}
		
		if (this.client.isConnected()) {
			if (this.cpu.process(this.client.info("cpu"))) {
				this.json += this.cpu.toJSON();
			}
			if (this.memory.process(this.client.info("memory"))) {
				if (!this.json.isEmpty()) {
					this.json += ",";
				}
				this.json += this.memory.toJSON();
			}
			if (this.system.process(this.client.info("sysinfo"))) {
				if (!this.json.isEmpty()) {
					this.json += ",";
				}
				this.json += this.system.toJSON();
			}
			if (this.stats.process(this.client.info("stats"))) {
				if (!this.json.isEmpty()) {
					this.json += ",";
				}
				this.json += this.stats.toJSON();
			}
		}
	}
	
	public String toJSON() {
		return this.json;
	}
	
	public String clusterInfo() {
		return this.client.clusterNodes();
	}

	public static void main(String[] args) {
		RedisInfo redis = new RedisInfo("192.168.177.61", 10021);
		redis.update();
		System.out.println(redis.toJSON());
	}
}
