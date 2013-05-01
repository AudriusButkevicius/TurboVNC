/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * Copyright 2009-2011 Pierre Ossman for Cendio AB
 * Copyright (C) 2011 Brian P. Hinz
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

package com.turbovnc.rfb;

import com.turbovnc.rdr.*;

abstract public class CMsgWriter {

  abstract public void writeClientInit(boolean shared);

  synchronized public void writeSetPixelFormat(PixelFormat pf)
  {
    startMsg(MsgTypes.msgTypeSetPixelFormat);
    os.pad(3);
    pf.write(os);
    endMsg();
  }

  synchronized public void writeSetEncodings(int nEncodings, int[] encodings)
  {
    startMsg(MsgTypes.msgTypeSetEncodings);
    os.skip(1);
    os.writeU16(nEncodings);
    for (int i = 0; i < nEncodings; i++)
      os.writeU32(encodings[i]);
    endMsg();
  }

  // Ask for encodings based on which decoders are supported.  Assumes higher
  // encoding numbers are more desirable.

  synchronized public void writeSetEncodings(int preferredEncoding,
                                             int lastEncoding, Options opts)
  {
    int nEncodings = 0;
    int[] encodings = new int[Encodings.encodingMax+3];
    if (opts.cursorShape)
      encodings[nEncodings++] = Encodings.pseudoEncodingCursor;
    if (cp.supportsDesktopResize)
      encodings[nEncodings++] = Encodings.pseudoEncodingDesktopSize;
    if (cp.supportsExtendedDesktopSize)
      encodings[nEncodings++] = Encodings.pseudoEncodingExtendedDesktopSize;
    if (cp.supportsDesktopRename)
      encodings[nEncodings++] = Encodings.pseudoEncodingDesktopName;
    if (cp.supportsClientRedirect)
      encodings[nEncodings++] = Encodings.pseudoEncodingClientRedirect;

    encodings[nEncodings++] = Encodings.pseudoEncodingLastRect;
    if (opts.continuousUpdates) {
      encodings[nEncodings++] = Encodings.pseudoEncodingContinuousUpdates;
      encodings[nEncodings++] = Encodings.pseudoEncodingFence;
    }

    if (Decoder.supported(preferredEncoding)) {
      encodings[nEncodings++] = preferredEncoding;
    }

    if (opts.copyRect) {
      encodings[nEncodings++] = Encodings.encodingCopyRect;
    }

    /*
     * Prefer encodings in this order:
     *
     *   Tight, ZRLE, Hextile, *
     */

    if ((preferredEncoding != Encodings.encodingTight) &&
        Decoder.supported(Encodings.encodingTight))
      encodings[nEncodings++] = Encodings.encodingTight;

    if ((preferredEncoding != Encodings.encodingZRLE) &&
        Decoder.supported(Encodings.encodingZRLE))
      encodings[nEncodings++] = Encodings.encodingZRLE;

    if ((preferredEncoding != Encodings.encodingHextile) &&
        Decoder.supported(Encodings.encodingHextile))
      encodings[nEncodings++] = Encodings.encodingHextile;

    // Remaining encodings
    for (int i = Encodings.encodingMax; i >= 0; i--) {
      switch (i) {
      case Encodings.encodingTight:
      case Encodings.encodingZRLE:
      case Encodings.encodingHextile:
        break;
      default:
        if ((i != preferredEncoding) && Decoder.supported(i))
            encodings[nEncodings++] = i;
      }
    }

    encodings[nEncodings++] = Encodings.pseudoEncodingLastRect;
    if (opts.compressLevel >= 0 && opts.compressLevel <= 9)
      encodings[nEncodings++] = Encodings.pseudoEncodingCompressLevel0
        + opts.compressLevel;
    if (opts.allowJpeg && opts.preferredEncoding == Encodings.encodingTight) {
      int qualityLevel = opts.quality / 10;
      if (qualityLevel > 9) qualityLevel = 9;
      encodings[nEncodings++] = Encodings.pseudoEncodingQualityLevel0
        + qualityLevel;
      encodings[nEncodings++] = Encodings.pseudoEncodingFineQualityLevel0
        + opts.quality;
      encodings[nEncodings++] = Encodings.pseudoEncodingSubsamp1X
        + opts.subsampling;
    } else if (opts.preferredEncoding != Encodings.encodingTight ||
               (lastEncoding >= 0 &&
                lastEncoding != Encodings.encodingTight)) {
      int qualityLevel = opts.quality;
      if (qualityLevel > 9) qualityLevel = 9;
      encodings[nEncodings++] = Encodings.pseudoEncodingQualityLevel0
        + qualityLevel;
    }

    writeSetEncodings(nEncodings, encodings);
  }

  synchronized public void writeFramebufferUpdateRequest(Rect r, boolean incremental)
  {
    startMsg(MsgTypes.msgTypeFramebufferUpdateRequest);
    os.writeU8(incremental?1:0);
    os.writeU16(r.tl.x);
    os.writeU16(r.tl.y);
    os.writeU16(r.width());
    os.writeU16(r.height());
    endMsg();
  }

  synchronized public void writeKeyEvent(int key, boolean down)
  {
    startMsg(MsgTypes.msgTypeKeyEvent);
    os.writeU8(down?1:0);
    os.pad(2);
    os.writeU32(key);
    endMsg();
  }

  synchronized public void writePointerEvent(Point pos, int buttonMask)
  {
    Point p = new Point(pos.x,pos.y);
    if (p.x < 0) p.x = 0;
    if (p.y < 0) p.y = 0;
    if (p.x >= cp.width) p.x = cp.width - 1;
    if (p.y >= cp.height) p.y = cp.height - 1;

    startMsg(MsgTypes.msgTypePointerEvent);
    os.writeU8(buttonMask);
    os.writeU16(p.x);
    os.writeU16(p.y);
    endMsg();
  }

  synchronized public void writeClientCutText(String str, int len)
  {
    startMsg(MsgTypes.msgTypeClientCutText);
    os.pad(3);
    os.writeU32(len);
    try {
      byte[] utf8str = str.getBytes("UTF8");
      os.writeBytes(utf8str, 0, len);
    } catch(java.io.UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    endMsg();
  }

  abstract public void startMsg(int type);
  abstract public void endMsg();

  synchronized public void setOutStream(OutStream os_) { os = os_; }

  ConnParams getConnParams() { return cp; }
  OutStream getOutStream() { return os; }

  protected CMsgWriter(ConnParams cp_, OutStream os_) {
    cp = cp_;  os = os_;
  }

  ConnParams cp;
  Options opts;
  OutStream os;

  static LogWriter vlog = new LogWriter("CMsgWriter");
}
