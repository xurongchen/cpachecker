#include<assert.h>

extern int __VERIFIER_nondet_int();

int fib(int a);

int main() {

  int input = __VERIFIER_nondet_int();

  int out = fib(input);
  assert(out >= 1);
}

int fib(int a) {
  return fib(a - 1) + fib(a - 2);
}
