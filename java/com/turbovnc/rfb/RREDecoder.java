/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
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

public class RREDecoder extends Decoder {

  public RREDecoder(CMsgReader reader_) { reader = reader_; }

  public void readRect(Rect r, CMsgHandler handler) {
    InStream is = reader.getInStream();
    int bytesPerPixel = handler.cp.pf().bpp / 8;
    boolean bigEndian = handler.cp.pf().bigEndian;
    int nSubrects = is.readU32();
    int bg = is.readPixel(bytesPerPixel, bigEndian);
    handler.fillRect(r, bg);

    for (int i = 0; i < nSubrects; i++) {
      int pix = is.readPixel(bytesPerPixel, bigEndian);
      int x = is.readU16();
      int y = is.readU16();
      int w = is.readU16();
      int h = is.readU16();
      handler.fillRect(new Rect(r.tl.x + x, r.tl.y + y,
                                r.tl.x + x + w, r.tl.y + y + h), pix);
    }
  }

  CMsgReader reader;
}
