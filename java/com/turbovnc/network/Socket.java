/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * Copyright (C) 2012 Brian P. Hinz
 * Copyright (C) 2012 D. R. Commander.  All Rights Reserved.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */

// -=- Socket - abstract base-class for any kind of network stream/socket

package com.turbovnc.network;

import com.turbovnc.rdr.*;
import java.nio.channels.*;

public abstract class Socket {

  public Socket(FileDescriptor fd) {
    instream = new FdInStream(fd);
    outstream = new FdOutStream(fd);
    ownStreams = true; isShutdown_ = false;
    queryConnection = false;
  }

  public FdInStream inStream() { return instream; }
  public FdOutStream outStream() { return outstream; }
  public FileDescriptor getFd() { return outstream.getFd(); }

  // if shutdown() is overridden then the override MUST call on to here
  public void shutdown() { isShutdown_ = true; }
  public void close() { getFd().close(); }
  public final boolean isShutdown() { return isShutdown_; }

  // information about this end of the socket
  public abstract int getMyPort();

  // information about the remote end of the socket
  public abstract String getPeerAddress(); // a string e.g. "192.168.0.1"
  public abstract String getPeerName();
  public abstract int getPeerPort();
  public abstract String getPeerEndpoint(); // <address>::<port>

  // Is the remote end on the same machine?
  public abstract boolean sameMachine();

  // Was there a "?" in the ConnectionFilter used to accept this Socket?
  public void setRequiresQuery() { queryConnection = true; }
  public final boolean requiresQuery() { return queryConnection; }

  protected Socket() {
    instream = null; outstream = null; ownStreams = false;
    isShutdown_ = false; queryConnection = false;
  }

  protected Socket(FdInStream i, FdOutStream o, boolean own) {
    instream = i; outstream = o; ownStreams = own;
    isShutdown_ = false; queryConnection = false;
  }

  protected FdInStream instream;
  protected FdOutStream outstream;
  boolean ownStreams;
  boolean isShutdown_;
  boolean queryConnection;
}

/*
abstract class ConnectionFilter {
  public abstract boolean verifyConnection(Socket s);
};

abstract class SocketListener {
  public SocketListener() {
    fd = null; filter = null;
  }

  // shutdown() stops the socket from accepting further connections
  public abstract void shutdown();

  // accept() returns a new Socket object if there is a connection
  // attempt in progress AND if the connection passes the filter
  // if one is installed.  Otherwise, returns 0.
  public abstract Socket accept();

  // setFilter() applies the specified filter to all new connections
  public void setFilter(ConnectionFilter f) {filter = f;}
  //public SocketDescriptor getFd() {return fd;}
  protected FileDescriptor fd;
  protected ConnectionFilter filter;
};
*/
