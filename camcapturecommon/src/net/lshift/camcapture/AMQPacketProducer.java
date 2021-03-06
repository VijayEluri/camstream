/*
 *  Camstream, an AMQP-based video streaming toolkit.
 *  Copyright (C) 2007-2009 LShift Ltd. <query@lshift.net>
 *  Copyright (C) 2010-2012 Tony Garnock-Jones <tonygarnockjones@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package net.lshift.camcapture;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.text.NumberFormat;

public class AMQPacketProducer {
    public String host;
    public String exchange;
    public String routingKey;

    public Connection conn;
    public Channel ch;

    public long frameCount;
    public long byteCount;
    public long startTime;

    public static final NumberFormat nf2dp = NumberFormat.getInstance();
    static {
        nf2dp.setMinimumFractionDigits(2);
        nf2dp.setMaximumFractionDigits(2);
        nf2dp.setGroupingUsed(false);
    }

    public AMQPacketProducer(String host, String exchange, String routingKey)
        throws IOException
    {
        this.host = host;
        this.exchange = exchange;
        this.routingKey = routingKey;

        ConnectionFactory cf = new ConnectionFactory();
        cf.setHost(host);
        this.conn = cf.newConnection();

	this.ch = conn.createChannel();

	ch.exchangeDeclare(exchange, "fanout");
    }

    public void resetStatistics() {
	this.frameCount = 0;
	this.byteCount = 0;
	this.startTime = System.currentTimeMillis();
    }

    public void publishPacket(BasicProperties prop, byte[] packet)
	throws IOException
    {
	this.byteCount += packet.length;
	this.frameCount++;
	ch.basicPublish(this.exchange, routingKey, prop, packet);
    }

    public void reportStatistics(String label) {
	long now = System.currentTimeMillis();
	double deltaSec = (now - startTime) / 1000.0;
	if (deltaSec > 0) {
	    double rate = byteCount / deltaSec;
	    double fps = frameCount / deltaSec;
	    System.out.println(label + "#" + frameCount + ", " +
			       nf2dp.format(fps) + " fps; " +
			       byteCount + " total -> " + nf2dp.format(rate) + " bytes/sec");
	}
    }
}
