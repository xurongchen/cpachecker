// used to test the ObserverAutomaton "LockingAutomaton"

int locked;
int x;

int lock() {
	locked = 1;
	return 0;
}
int lock(int byID) {
	locked = 1;
	return 0;
}

int unlock(int byID) {
	locked = 0;
	return 0;
}
int unlock() {
	locked = 0;
	return 0;
}

int init() {
	locked = 0;
	return 0;
}

int main() {
	int myId = 10;

	init();

	lock(myId);

	x = 0;


	if (y != 1) {
	ERROR:
			goto ERROR;
		}

	unlock(myId);

	if (y != 0) {
		ERROR:
				goto ERROR;
			}

	lock(myId); // remains locked. This should result in a Warning?

	x  = /* comment */  x +   1 ;

	if (y != 1) {
		ERROR:
				goto ERROR;
			}


	//return 0;
}
