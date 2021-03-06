/* Generated by CIL v. 1.3.7 */
/* print_CIL_Input is true */

#line 211 "/usr/lib/gcc/x86_64-linux-gnu/4.4.3/include/stddef.h"
typedef unsigned long size_t;
#line 141 "/usr/include/bits/types.h"
typedef long __off_t;
#line 142 "/usr/include/bits/types.h"
typedef long __off64_t;
#line 45 "/usr/include/stdio.h"
struct _IO_FILE;
#line 45
struct _IO_FILE;
#line 49 "/usr/include/stdio.h"
typedef struct _IO_FILE FILE;
#line 170 "/usr/include/libio.h"
struct _IO_FILE;
#line 180 "/usr/include/libio.h"
typedef void _IO_lock_t;
#line 186 "/usr/include/libio.h"
struct _IO_marker {
   struct _IO_marker *_next ;
   struct _IO_FILE *_sbuf ;
   int _pos ;
};
#line 271 "/usr/include/libio.h"
struct _IO_FILE {
   int _flags ;
   char *_IO_read_ptr ;
   char *_IO_read_end ;
   char *_IO_read_base ;
   char *_IO_write_base ;
   char *_IO_write_ptr ;
   char *_IO_write_end ;
   char *_IO_buf_base ;
   char *_IO_buf_end ;
   char *_IO_save_base ;
   char *_IO_backup_base ;
   char *_IO_save_end ;
   struct _IO_marker *_markers ;
   struct _IO_FILE *_chain ;
   int _fileno ;
   int _flags2 ;
   __off_t _old_offset ;
   unsigned short _cur_column ;
   signed char _vtable_offset ;
   char _shortbuf[1] ;
   _IO_lock_t *_lock ;
   __off64_t _offset ;
   void *__pad1 ;
   void *__pad2 ;
   void *__pad3 ;
   void *__pad4 ;
   size_t __pad5 ;
   int _mode ;
   char _unused2[(15UL * sizeof(int ) - 4UL * sizeof(void *)) - sizeof(size_t )] ;
};
#line 339 "/usr/include/stdio.h"
extern int printf(char const   * __restrict  __format  , ...) ;
#line 127 "/usr/include/string.h"
extern  __attribute__((__nothrow__)) char *strcpy(char * __restrict  __dest , char const   * __restrict  __src )  __attribute__((__nonnull__(1,2))) ;
#line 397
extern  __attribute__((__nothrow__)) size_t strlen(char const   *__s )  __attribute__((__pure__,
__nonnull__(1))) ;
#line 71 "/usr/include/assert.h"
extern  __attribute__((__nothrow__, __noreturn__)) void __assert_fail(char const   *__assertion ,
                                                                      char const   *__file ,
                                                                      unsigned int __line ,
                                                                      char const   *__function ) ;
#line 98 "call_fb_realpath.c"
extern int ( /* missing proto */  fb_realpath)() ;
#line 85 "call_fb_realpath.c"
int main(int argc , char **argv ) 
{ char resolved_path[46] ;
  char path[100] ;
  size_t tmp ;
  char const   * __restrict  __cil_tmp7 ;
  unsigned long __cil_tmp8 ;
  unsigned long __cil_tmp9 ;
  char *__cil_tmp10 ;
  char * __restrict  __cil_tmp11 ;
  char **__cil_tmp12 ;
  char *__cil_tmp13 ;
  char const   * __restrict  __cil_tmp14 ;
  unsigned long __cil_tmp15 ;
  unsigned long __cil_tmp16 ;
  char *__cil_tmp17 ;
  char const   *__cil_tmp18 ;
  char const   * __restrict  __cil_tmp19 ;
  unsigned long __cil_tmp20 ;
  unsigned long __cil_tmp21 ;
  char *__cil_tmp22 ;
  char const   * __restrict  __cil_tmp23 ;
  unsigned long __cil_tmp24 ;
  unsigned long __cil_tmp25 ;
  char *__cil_tmp26 ;
  unsigned long __cil_tmp27 ;
  unsigned long __cil_tmp28 ;
  char *__cil_tmp29 ;

  {
  {
#line 90
  __cil_tmp7 = (char const   * __restrict  )"MAXPATHLEN=%d\n";
#line 90
  printf(__cil_tmp7, 46);
  }
#line 92
  if (argc == 2) {

  } else {
    {
#line 92
    __assert_fail("argc==2", "call_fb_realpath.c", 92U, "main");
    }
  }
  {
#line 94
  __cil_tmp8 = 0 * 1UL;
#line 94
  __cil_tmp9 = (unsigned long )(path) + __cil_tmp8;
#line 94
  __cil_tmp10 = (char *)__cil_tmp9;
#line 94
  __cil_tmp11 = (char * __restrict  )__cil_tmp10;
#line 94
  __cil_tmp12 = argv + 1;
#line 94
  __cil_tmp13 = *__cil_tmp12;
#line 94
  __cil_tmp14 = (char const   * __restrict  )__cil_tmp13;
#line 94
  strcpy(__cil_tmp11, __cil_tmp14);
#line 96
  __cil_tmp15 = 0 * 1UL;
#line 96
  __cil_tmp16 = (unsigned long )(path) + __cil_tmp15;
#line 96
  __cil_tmp17 = (char *)__cil_tmp16;
#line 96
  __cil_tmp18 = (char const   *)__cil_tmp17;
#line 96
  tmp = strlen(__cil_tmp18);
#line 96
  __cil_tmp19 = (char const   * __restrict  )"Input path = %s, strlen(path) = %d\n";
#line 96
  __cil_tmp20 = 0 * 1UL;
#line 96
  __cil_tmp21 = (unsigned long )(path) + __cil_tmp20;
#line 96
  __cil_tmp22 = (char *)__cil_tmp21;
#line 96
  printf(__cil_tmp19, __cil_tmp22, tmp);
#line 97
  __cil_tmp23 = (char const   * __restrict  )"MAXPATHLEN = %d\n";
#line 97
  printf(__cil_tmp23, 46);
#line 98
  __cil_tmp24 = 0 * 1UL;
#line 98
  __cil_tmp25 = (unsigned long )(path) + __cil_tmp24;
#line 98
  __cil_tmp26 = (char *)__cil_tmp25;
#line 98
  __cil_tmp27 = 0 * 1UL;
#line 98
  __cil_tmp28 = (unsigned long )(resolved_path) + __cil_tmp27;
#line 98
  __cil_tmp29 = (char *)__cil_tmp28;
#line 98
  fb_realpath(__cil_tmp26, __cil_tmp29);
  }
#line 100
  return (0);
}
}
