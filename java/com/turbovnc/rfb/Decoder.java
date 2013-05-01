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

package com.turbovnc.rfb;

public abstract class Decoder {

  public abstract void readRect(Rect r, CMsgHandler handler);

  public void reset() {}

  public static boolean supported(int encoding) {
/*
    return encoding <= Encodings.encodingMax && createFns[encoding];
*/
    return (encoding == Encodings.encodingRaw ||
            encoding == Encodings.encodingRRE ||
            encoding == Encodings.encodingHextile ||
            encoding == Encodings.encodingTight ||
            encoding == Encodings.encodingZRLE);
  }

  public static Decoder createDecoder(int encoding, CMsgReader reader) {
/*
    if (encoding <= Encodings.encodingMax && createFns[encoding])
      return (createFns[encoding])(reader);
    return 0;
*/
    switch(encoding) {
      case Encodings.encodingRaw:     return new RawDecoder(reader);
      case Encodings.encodingRRE:     return new RREDecoder(reader);
      case Encodings.encodingHextile: return new HextileDecoder(reader);
      case Encodings.encodingTight:   return new TightDecoder(reader);
      case Encodings.encodingZRLE:    return new ZRLEDecoder(reader);
    }
    return null;
  }
}
