/* Copyright (C)2004 Landmark Graphics Corporation
 * Copyright (C)2005, 2006 Sun Microsystems, Inc.
 *
 * The VNC system is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */

// FBX -- the fast Frame Buffer eXchange library
// This library is designed to facilitate transferring pixels to/from the framebuffer using fast
// 2D O/S-native methods that do not rely on OpenGL acceleration

#ifndef __FBX_H__
#define __FBX_H__

#define USESHM
#if defined(_WIN32) && !defined(FBXX11)
 #define FBXWIN32
#endif

#include <stdio.h>
#ifdef FBXWIN32
 #include <windows.h>
 typedef HDC fbx_gc;
 typedef HWND fbx_wh;
#else
 #ifdef FBXX11
  #include <Xwindows.h>
 #endif
 #ifdef XDK
  #define NOREDIRECT
 #endif
 #include <X11/Xlib.h>
 #ifdef USESHM
  #ifdef XDK
   #include <X11/hclshm.h>
   // Exceed likes to redefine stdio, so we un-redefine it :/
   #undef fprintf
   #undef printf
   #undef putchar
   #undef putc
   #undef puts
   #undef fputc
   #undef fputs
   #undef perror
  #elif !defined(XWIN32)
   #include <sys/ipc.h>
   #include <sys/shm.h>
  #endif
  #include <X11/extensions/XShm.h>
 #endif
 #include <X11/Xutil.h>
 typedef GC fbx_gc;
 typedef struct {Display *dpy; Window win;} fbx_wh;
#endif

#define BMPPAD(pitch) ((pitch+(sizeof(int)-1))&(~(sizeof(int)-1)))

#define FBX_FORMATS 7
enum {FBX_RGB, FBX_RGBA, FBX_BGR, FBX_BGRA, FBX_ABGR, FBX_ARGB, FBX_INDEX};  // pixel formats

static const int fbx_ps[FBX_FORMATS]=
	{3, 4, 3, 4, 4, 4, 1};
static const int fbx_bgr[FBX_FORMATS]=
	{0, 0, 1, 1, 1, 0, 0};
static const int fbx_alphafirst[FBX_FORMATS]=
	{0, 0, 0, 0, 1, 1, 0};
static const int fbx_roffset[FBX_FORMATS]=
	{0, 0, 2, 2, 3, 1, 0};
static const int fbx_goffset[FBX_FORMATS]=
	{1, 1, 1, 1, 2, 2, 0};
static const int fbx_boffset[FBX_FORMATS]=
	{2, 2, 0, 0, 1, 3, 0};

typedef struct _fbx_struct
{
	int width, height, pitch;
	char *bits;
	int format;
	fbx_wh wh;
	int shm;

	#ifdef FBXWIN32
	HDC hmdc;  HBITMAP hdib;
	#else
	#ifdef USESHM
	#ifdef XWIN32
	HANDLE filemap;
	#endif
	XShmSegmentInfo shminfo;  int xattach;
	#endif
	GC xgc;
	XImage *xi;
	Pixmap pm;
	#endif
} fbx_struct;

#ifdef __cplusplus
extern "C" {
#endif

/////////////////////////////////////////////////////////////
// All of these methods return -1 on failure or 0 on success.
/////////////////////////////////////////////////////////////

/*
  fbx_init
  (fbx_struct *s, fbx_wh wh, int width, int height, int useshm)

  s = Address of fbx_struct (must be pre-allocated by user)
  wh = Handle to the window that you wish to read from or write to.  On Windows, this is the same
       as the HWND.  On Unix, this is a struct {Display *dpy, Window win} that describes the X11
       display and window you wish to use
  width = Width of buffer (in pixels) that you wish to create.  0 = use width of window
  height = Height of buffer (in pixels) that you wish to create.  0 = use height of window
  useshm = Use MIT-SHM extension, if available (Unix only)

  NOTES:
  -- fbx_init() is idempotent.  If you call it multiple times, it will re-initialize the
     buffer only when it is necessary to do so (such as when the window size has changed.)
  -- On Windows, fbx_init() will return a buffer configured with the same pixel format as the
     screen, unless the screen depth is < 24 bits, in which case it will always return a 32-bit BGRA
     buffer.

  On return, fbx_init() fills in the following relevant information in the fbx_struct that you
  passed to it:
  s->format = pixel format of the buffer (one of FBX_RGB, FBX_RGBA, FBX_BGR, FBX_BGRA, FBX_ABGR,
              or FBX_ARGB)
  s->width, s->height = dimensions of the buffer
  s->pitch = bytes in each scanline of the buffer
  s->bits = address of the start of the buffer
*/
int fbx_init(fbx_struct *s, fbx_wh wh, int width, int height, int useshm);

/*
  fbx_read
  (fbx_struct *s, int x, int y)

  This routine copies pixels from the framebuffer into the memory buffer specified by s

  s = Address of fbx_struct previously initialized by a call to fbx_init()
  x = Horizontal offset (from left of window client area) of rectangle to read
  y = Vertical offset (from top of window client area) of rectangle to read
  NOTE: width and height of rectangle are not adjustable without re-calling fbx_init()

  On return, s->bits contains a facsimile of the window's pixels
*/
int fbx_read(fbx_struct *s, int x, int y);

/*
  fbx_write
  (fbx_struct *s, int bmpx, int bmpy, int winx, int winy, int w, int h)

  This routine copies pixels from the memory buffer specified by s to the framebuffer

  s = Address of fbx_struct previously initialized by a call to fbx_init()
      s->bits should contain the pixels you wish to blit
  bmpx = left offset of the region you wish to blit (relative to the memory buffer)
  bmpy = top offset of the region you wish to blit (relative to the memory buffer)
  winx = left offset of where you want the pixels to end up (relative to window client area)
  winy = top offset of where you want the pixels to end up (relative to window client area)
  w = width of region you wish to blit (0 = whole bitmap)
  h = height of region you wish to blit (0 = whole bitmap)
*/
int fbx_write (fbx_struct *s, int bmpx, int bmpy, int winx, int winy, int w, int h);

/*
  fbx_awrite
  (fbx_struct *s, int bmpx, int bmpy, int winx, int winy, int w, int h)

  Same as fbx_write, but asynchronous.  The write isn't guaranteed to complete
  until fbx_sync() is called.  On Windows, fbx_awrite is the same as fbx_write.
*/
#ifdef FBXWIN32
#define fbx_awrite fbx_write
#else
int fbx_awrite (fbx_struct *s, int bmpx, int bmpy, int winx, int winy, int w, int h);
#endif

/*
  fbx_flip
  (fbx_struct *s, int bmpx, int bmpy, int w, int h)

  This routine performs an in-place vertical flip of the region of interest specified by
  bmpx, bmpy, w, and h in the memory buffer specified by s

  s = Address of fbx_struct previously initialized by a call to fbx_init()
      s->bits should contain the pixels you wish to flip
  bmpx = left offset of the region you wish to flip (relative to the memory buffer)
  bmpy = top offset of the region you wish to flip (relative to the memory buffer)
  w = width of region you wish to flip (0 = whole bitmap)
  h = height of region you wish to flip (0 = whole bitmap)
*/
int fbx_flip(fbx_struct *s, int bmpx, int bmpy, int w, int h);

/*
  fbx_sync
  (fbx_struct *s)

  Complete a previous asynchronous write.  On Windows, this does nothing.
*/
int fbx_sync (fbx_struct *s);

/*
  fbx_term
  (fbx_struct *s)

  Free the memory buffers pointed to by structure s

  NOTE: this routine is idempotent.  It only frees stuff that needs freeing.
*/
int fbx_term(fbx_struct *s);

/*
  fbx_geterrmsg

  This returns a string containing the reason why the last command failed
*/
char *fbx_geterrmsg(void);

/*
  fbx_geterrline

  This returns the line (within fbx.c) of the last failure
*/
int fbx_geterrline(void);

/*
  fbx_formatname

  Returns a character string describing the pixel format specified in the
  format parameter
*/
const char *fbx_formatname(int format);

/*
  fbx_printwarnings
  (FILE *output_stream)

  By default, FBX will not print warning messages (such as messages related to
  its automatic selection of a particular drawing method.)  These messages are
  sometimes useful when diagnosing performance issues.  Passing a stream
  pointer (such as stdout, stderr, or a pointer returned from a previous call
  to fopen()) to this function will enable warning messages and will cause them
  to be printed to the specified stream.  Passing an argument of NULL to this
  function will disable warnings.
*/
void fbx_printwarnings(FILE *output_stream);

#ifdef __cplusplus
}
#endif

#endif // __FBX_H__
