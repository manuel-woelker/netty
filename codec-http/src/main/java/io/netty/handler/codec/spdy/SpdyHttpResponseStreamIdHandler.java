/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.spdy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.HttpMessage;

import java.util.LinkedList;
import java.util.Queue;

/**
 * {@link MessageToMessageCodec} that takes care of adding the right {@link SpdyHttpHeaders.Names#STREAM_ID} to the
 * {@link HttpMessage} if one is not present. This makes it possible to just re-use plan handlers current used
 * for HTTP.
 */
public class SpdyHttpResponseStreamIdHandler extends
        MessageToMessageCodec<Object, HttpMessage> {
    private static final Integer NO_ID = -1;
    private final Queue<Integer> ids = new LinkedList<Integer>();

    public SpdyHttpResponseStreamIdHandler() {
        super(new Class<?>[] { HttpMessage.class, SpdyRstStreamFrame.class }, new Class<?>[] { HttpMessage.class });
    }

    @Override
    protected Object encode(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
        Integer id = ids.poll();
        if (id != null && id.intValue() != NO_ID && !msg.headers().contains(SpdyHttpHeaders.Names.STREAM_ID)) {
            SpdyHttpHeaders.setStreamId(msg, id);
        }
        return msg;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpMessage) {
            boolean contains = ((HttpMessage) msg).headers().contains(SpdyHttpHeaders.Names.STREAM_ID);
            if (!contains) {
                ids.add(NO_ID);
            } else {
                ids.add(SpdyHttpHeaders.getStreamId((HttpMessage) msg));
            }
        } else if (msg instanceof SpdyRstStreamFrame) {
            ids.remove(((SpdyRstStreamFrame) msg).getStreamId());
        }

        return msg;
    }

    @Override
    protected void freeInboundMessage(Object msg) throws Exception {
        // just pass through so no free
    }

    @Override
    protected void freeOutboundMessage(HttpMessage msg) throws Exception {
        // just pass through so no free
    }
}
