/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
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

//
// CMsgReader - class for reading RFB messages on the client side
// (i.e. messages from server to client).
//

package com.turbovnc.rfb;

import com.turbovnc.rdr.*;

public abstract class CMsgReader {

  protected CMsgReader(CMsgHandler handler_, InStream is_) {
    imageBufIdealSize = 0;
    handler = handler_;
    is = is_;
    imageBuf = null;
    imageBufSize = 0;
    decoders = new Decoder[Encodings.encodingMax + 1];
  }

  protected void readSetColourMapEntries() {
    is.skip(1);
    int firstColour = is.readU16();
    int nColours = is.readU16();
    int[] rgbs = new int[nColours * 3];
    for (int i = 0; i < nColours * 3; i++)
      rgbs[i] = is.readU16();
    handler.setColourMapEntries(firstColour, nColours, rgbs);
  }

  protected void readBell() {
    handler.bell();
  }

  protected void readServerCutText() {
    is.skip(3);
    int len = is.readU32();
    if (len > 256 * 1024) {
      is.skip(len);
      vlog.error("cut text too long (" + len + " bytes) - ignoring");
      return;
    }
    byte[] buf = new byte[len];
    is.readBytes(buf, 0, len);
    String str = new String();
    try {
      str = new String(buf, "UTF8");
    } catch(java.io.UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    handler.serverCutText(str, len);
  }

  protected void readFramebufferUpdateStart() {
    handler.framebufferUpdateStart();
  }

  protected void readFramebufferUpdateEnd() {
    handler.framebufferUpdateEnd();
  }

  protected void readRect(Rect r, int encoding) {
    if ((r.br.x > handler.cp.width) || (r.br.y > handler.cp.height)) {
      vlog.error("Rect too big: " + r.width() + "x" + r.height() + " at " +
                  r.tl.x + "," + r.tl.y + " exceeds " + handler.cp.width +
                  "x" + handler.cp.height);
      throw new ErrorException("Rect too big");
    }

    if (r.isEmpty())
      vlog.error("Ignoring zero size rect");

    handler.beginRect(r, encoding);

    if (encoding == Encodings.encodingCopyRect) {
      readCopyRect(r);
    } else {

      if (decoders[encoding] == null) {
        decoders[encoding] = Decoder.createDecoder(encoding, this);
        if (decoders[encoding] == null) {
          vlog.error("Unknown rect encoding " + encoding);
          throw new ErrorException("Unknown rect encoding" + encoding);
        }
      }
      handler.startDecodeTimer();
      decoders[encoding].readRect(r, handler);
      handler.stopDecodeTimer();
    }

    handler.endRect(r, encoding);
  }

  protected void readCopyRect(Rect r) {
    int srcX = is.readU16();
    int srcY = is.readU16();
    handler.copyRect(r, srcX, srcY);
  }

  protected void readSetCursor(int width, int height, Point hotspot) {
    int dataLen = width * height;
    int maskLen = ((width + 7) / 8) * height;
    int[] data = new int[dataLen];
    byte[] mask = new byte[maskLen];

    is.readPixels(data, dataLen, (handler.cp.pf().bpp / 8),
                  handler.cp.pf().bigEndian);
    is.readBytes(mask, 0, maskLen);

    handler.setCursor(width, height, hotspot, data, mask);
  }

  public int[] getImageBuf(int required) {
    return getImageBuf(required, 0, 0);
  }

  public int[] getImageBuf(int required, int requested, int nPixels) {
    int requiredBytes = required;
    int requestedBytes = requested;
    int size = requestedBytes;
    if (size > imageBufIdealSize) size = imageBufIdealSize;

    if (size < requiredBytes)
      size = requiredBytes;

    if (imageBufSize < size) {
      imageBufSize = size;
      imageBuf = new int[imageBufSize];
    }
    if (nPixels != 0)
      nPixels = imageBufSize / (handler.cp.pf().bpp / 8);
    return imageBuf;
  }

  public final int bpp() {
    return handler.cp.pf().bpp;
  }

  public final boolean isTurboJPEG() {
    Decoder d = decoders[Encodings.encodingTight];
    if (d instanceof TightDecoder && d != null)
      return ((TightDecoder)d).isTurboJPEG();
    return false;
  }

  public final void reset() {
    for (int i = 0; i < Encodings.encodingMax; i++) {
      if (decoders[i] != null)
        decoders[i].reset();
    }
  }

  public abstract void readServerInit(boolean benchmark);

  // readMsg() reads a message, calling the handler as appropriate.
  public abstract void readMsg();

  public InStream getInStream() { return is; }

  public int imageBufIdealSize;

  protected CMsgHandler handler;
  protected InStream is;
  protected Decoder[] decoders;
  protected int[] imageBuf;
  protected int imageBufSize;

  static LogWriter vlog = new LogWriter("CMsgReader");
}
