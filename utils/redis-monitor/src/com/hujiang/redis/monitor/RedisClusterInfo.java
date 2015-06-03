package com.hujiang.redis.monitor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import com.hujiang.redis.monitor.info.Info;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;

public class RedisClusterInfo extends Info {
	
	private HashMap<Integer, ClusterInfoNode> nodes		= new HashMap<Integer, ClusterInfoNode>();
	private HashMap<ClusterInfoNode, RedisInfo> nodeInfo= new HashMap<ClusterInfoNode, RedisInfo>();
	
	final static String IPv4_LOCALHOST	= "127.0.0.1";
	final static String IPv6_LOCALHOST	= "127.0.0.1";
	
	private String publicIP				= null;
	private Jedis client				= null;
	private SimpleDateFormat dateFormat	= new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	public RedisClusterInfo(final String host, int port) {
		this.publicIP	= host;
		this.client		= new Jedis(host, port);
	}
	
	public RedisClusterInfo(final String url) {
		this.parseUrl(url);
	}
	private void parseUrl(String url) {
		if (url == null || url.isEmpty()) {
			return;
		}
		
		String host	= null;
		int port	= 0;
		
		int index = 0, begin = 0, len = url.length();
		while (index < len) {
			if (url.charAt(index) == ':') {
				// Get the IP address.
				host = url.substring(begin, index);
				begin = index + 1;
				break;
			}
			else {
				index ++;
			}
		}
		port = Integer.parseInt(url.substring(begin, len));
		
		this.publicIP	= host;
		this.client		= new Jedis(host, port);
	}
	
	public void close() {
		if (this.nodeInfo != null) {
			for (Entry<ClusterInfoNode, RedisInfo> e: this.nodeInfo.entrySet()) {
				e.getValue().clear();
				e.getValue().close();
			}
			this.nodeInfo.clear();
		}
		
		if (this.client != null) {
			this.client.close();
		}
	}
	
	@Override
	public void clear() {
		if (this.nodes != null) {
			for (Entry<Integer, ClusterInfoNode> e: this.nodes.entrySet()) {
				e.getValue().clear();
			}
			this.nodes.clear();
		}
		
		this.s			= null;
		this.index		= 0;
		this.len		= 0;
	}
	
	public void update() {
		if (!this.client.isConnected()) {
			this.client.connect();
		}
		this.process(this.client.clusterNodes());
	}

	@Override
	public String toJSON() {
		String s = "{\"time\":\"" + this.dateFormat.format(new Date()) + "\",masters:[";
		
		boolean firstMaster = true;
		boolean firstSlave = true;
		ClusterInfoNode master = null;
		ClusterInfoNode slave = null;
		RedisInfo nodeInfo = null;
		for (Entry<Integer, ClusterInfoNode> e: this.nodes.entrySet()) {
			master = e.getValue();
			if (!master.master) {
				continue;
			}
			
			if (firstMaster) {
				firstMaster = false;
				s += "{";
			}
			else {
				s += ",{";
			}
			s += "\"ID\":\"" + master.ID + "\",";
			s += "\"host\":\"" + master.host + "\",";
			s += "\"port\":\"" + master.port + "\",";
			s += "\"slotStart\":\"" + master.slotStart + "\",";
			s += "\"slotEnd\":\"" + master.slotEnd + "\",";
			s += "\"connected\":\"" + master.connected + "\",";
			
			nodeInfo = this.nodeInfo.get(master);
			if (nodeInfo != null) {
				s += nodeInfo.toJSON() + ",";
			}
			
			s += "slaves:[";
			firstSlave = true;
			for (Entry<Integer, ClusterInfoNode> d: master.slaves.entrySet()) {
				slave = d.getValue();
				
				if (firstSlave) {
					firstSlave = false;
					s += "{";
				}
				else {
					s += ",{";
				}
				s += "\"ID\":\"" + slave.ID + "\",";
				s += "\"host\":\"" + slave.host + "\",";
				s += "\"port\":\"" + slave.port + "\",";
				s += "\"connected\":\"" + slave.connected + "\"";
				
				nodeInfo = this.nodeInfo.get(slave);
				if (nodeInfo != null) {
					s += "," + nodeInfo.toJSON();
				}
				
				s += "}";
			}
			s += "]}";
		}
		
		s += "]}";
		
		return s;
	}

	@Override
	public boolean process(String s) {
		if (!this.preprocess(s)) {
			return false;
		}
		
		HashMap<String, ClusterInfoNode> masterNodes = new HashMap<String, ClusterInfoNode>();
		ClusterInfoNode node = null;
		while (this.index < this.len) {
			node = new ClusterInfoNode();
			
			node.UUID = this.getNextToken();
			node.host = this.getNextToken();
			node.port = Integer.parseInt(this.getNextToken());
			
			node.master = this.getNextToken().contains("master");
			if (node.master) {
				this.getNextToken();
				this.getNextToken();
				
				node.ID			= Integer.parseInt(this.getNextToken());
				node.connected	= this.getNextToken().contains("disconnected") ? false : true;
				
				node.slotStart	= Integer.parseInt(this.getNextToken());
				node.slotEnd	= Integer.parseInt(this.getNextToken());
				
				masterNodes.put(node.UUID, node);
			}
			else {
				node.masterUUID = this.getNextToken();
				
				this.getNextToken();
				this.getNextToken();
				
				node.ID			= Integer.parseInt(this.getNextToken());
				node.connected	= this.getNextToken().contains("disconnected") ? false : true;
			}
			this.nodes.put(node.ID, node);
			
			this.skipALine();
		}
		
		// Update master-slave relation.
		if (!(masterNodes.isEmpty())) {
			for (Entry<Integer, ClusterInfoNode> e: this.nodes.entrySet()) {
				node = e.getValue();
				if (!node.master) {
					masterNodes.get(node.masterUUID).slaves.put(node.ID, node);
				}
			}
			masterNodes.clear();
		}
		
		// Update IP addresses.
		if ((this.publicIP != null) && (!this.publicIP.isEmpty())) {
			for (Entry<Integer, ClusterInfoNode> e: this.nodes.entrySet()) {
				node = e.getValue();
				if (node.host.compareTo(RedisClusterInfo.IPv4_LOCALHOST) == 0) {
					node.host = this.publicIP;
				}
			}
		}
		
		// Update local information for each Redis server.
		RedisInfo info = null;
		for (Entry<Integer, ClusterInfoNode> e: this.nodes.entrySet()) {
			node = e.getValue();
			info = this.nodeInfo.get(node);
			if (info == null) {
				info = new RedisInfo(node.host, node.port);
				this.nodeInfo.put(node, info);
			}
			info.update();
		}
		
		return true;
	}
	
	public Set<HostAndPort> getHostAndPorts() {
		HashSet<HostAndPort> result = new HashSet<HostAndPort>();
		
		ClusterInfoNode node = null;
		for (Entry<Integer, ClusterInfoNode> e: this.nodes.entrySet()) {
			node = e.getValue();
			result.add(new HostAndPort(node.host, node.port));
		}
		return result;
	}
	
	public static void main(String[] args) {	
		RedisClusterInfo info = new RedisClusterInfo("192.168.177.61", 10011);
		info.update();
		
		System.out.println(info.toJSON());
	}
	
	class ClusterInfoNode {
		public int		ID;
		public String	UUID;
		
		public String	host;
		public int		port;
		
		public int		slotStart	= 0;
		public int		slotEnd		= 0;
		
		public boolean	connected	= false;
		public boolean	master		= false;
		
		public String	masterUUID;
		public HashMap<Integer, ClusterInfoNode> slaves = new HashMap<Integer, ClusterInfoNode>();
		
		public void clear() {
			this.ID			= 0;
			this.UUID		= null;
			
			this.host		= null;
			this.ID			= 0;
			
			this.slotStart	= 0;
			this.slotEnd	= 0;
			
			this.connected	= false;
			this.master		= false;
			
			this.master		= false;
			if (this.slaves != null) {
				this.slaves.clear();
			}
		}
	}
}
