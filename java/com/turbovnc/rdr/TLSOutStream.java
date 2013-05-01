/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * Copyright (C) 2005 Martin Koegler
 * Copyright (C) 2010 TigerVNC Team
 * Copyright (C) 2011-2012 Brian P. Hinz
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

package com.turbovnc.rdr;

import java.nio.channels.*;
import javax.net.ssl.*;

import com.turbovnc.network.*;

public class TLSOutStream extends OutStream {

  static final int DEFAULT_BUF_SIZE = 16384;

  public TLSOutStream(OutStream out_, SSLEngineManager manager_) {
    manager = manager_;
    out = (FdOutStream)out_;
    SSLSession session = manager.getSession();
    bufSize = session.getApplicationBufferSize();
    b = new byte[bufSize];
    ptr = offset = start = 0;
    end = start + bufSize;
  }

  public int length() {
    return offset + ptr - start;
  }

  public void flush() {
    int sentUpTo = start;
    while (sentUpTo < ptr) {
      int n = writeTLS(b, sentUpTo, ptr - sentUpTo);
      sentUpTo += n;
      offset += n;
    }

    ptr = start;
    //out.flush();
  }

  protected int overrun(int itemSize, int nItems) {
    if (itemSize > bufSize)
      throw new ErrorException("TLSOutStream overrun: max itemSize exceeded");

    flush();

    if (itemSize * nItems > end - ptr)
      nItems = (end - ptr) / itemSize;

    return nItems;
  }

  protected int writeTLS(byte[] data, int dataPtr, int length) {
    int n = 0;

    try {
      n = manager.write(data, dataPtr, length);
    } catch(java.io.IOException e) {
      throw new ErrorException("TLS write error: " + e.getMessage());
    }
    //if (n == GNUTLS_E_INTERRUPTED || n == GNUTLS_E_AGAIN)
    //  return 0;

    //if (n < 0)
    //  throw new TLSException("writeTLS", n);

    return n;
  }

  private SSLEngineManager manager;
  private FdOutStream out;
  private int start;
  private int offset;
  private int bufSize;
}
