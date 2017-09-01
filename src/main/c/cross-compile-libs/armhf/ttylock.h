/*
 *	ttylock.h
 *	(c) 1997 by Fabrizio Polacco <fpolacco@debian.org>
 *	this header file is part of the lockdev library.
 *
 *	This program is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Library General Public
 *	License as published by the Free Software Foundation; version 2
 *	dated June, 1991.
 *
 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU Library General
 *	Public License along with this program;  if not, write to the
 *	Free Software Foundation, Inc., 675 Mass Ave., Cambridge, MA
 *	02139, USA.
 *
 *	This header file provides an alternative API to the services of
 *	the lockdev library, the same API implemented by AIX.
 *	It is intended as a quick help for any program originally
 *	designed for the AIX that wants to move to lockdev.
 *
 */

#ifndef _TTYLOCK_H_
#define _TTYLOCK_H_

#ifdef	__cplusplus
extern "C" {
#endif

#include <lockdev.h>

/* API of the library */

int ttylock (char * devname);
int ttywait (char * devname);
int ttyunlock (char * devname);
int ttylocked (char * devname);


static inline int
ttylock(char *devname)
{
	/* should set errno ? */
	return dev_lock( devname) == 0 ? 0 : -1;
}

static inline int
ttyunlock (char *devname)
{
	return dev_unlock(devname, 0);
}

static inline int
ttylocked(char *devname)
{
	return dev_testlock( devname) == 0 ? 0 : -1;
}

static inline int
ttywait (char *devname)
{
	while( dev_testlock( devname)) {
		sleep(1);
	}
}


#ifdef	__cplusplus
};
#endif

#endif /* _TTYLOCK_H_ */
