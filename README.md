Widely-Enhanced Redis
---------------------

What is WE-Redis?
-----------------
WE-Redis is a variant of Redis with non-official enhancements, to meet
some special requirements in Redis-relative systems. And therefore
we hope the deployment of WE-Redis will facilitate your design and development.

Currently, WE-Redis relies on Redis 3.0.1, and WE-Redis can be built
in the same way as Redis.

The latest version of WE-Redis is 3.0.1e.

What exactly are the enhancements?
----------------------------------

- Server-Side Distributed Lock

This enhancement provides 3 extra Redis commands, namely:

    TRYLOCK    mutex : Lock the mutex, only if it is currently unlocked.
    UNLOCK     mutex : Unlock the mutex, if it is currently owned by the calling client.
    LOCKSTATUS mutex : Get the status of the mutex.

Please read [doc/lock.md](doc/lock.md) to get more details.

- System Information

This enhancement provides 1 extra Redis command, namely:

	INFO sysinfo | INFO all

Please read [doc/sysinfo.md](doc/sysinfo.md) to get more details.

------------------------------------------------------------------------------

The original Redis README is also included in this file,
to pay our respects to Salvatore Sanfilippo, Pieter Noordhuis, and
all other contributors of Redis.

Where to find complete Redis documentation?
-------------------------------------------

This README is just a fast "quick start" document. You can find more detailed
documentation at http://redis.io

Building Redis
--------------

Redis can be compiled and used on Linux, OSX, OpenBSD, NetBSD, FreeBSD.
We support big endian and little endian architectures.

It may compile on Solaris derived systems (for instance SmartOS) but our
support for this platform is "best effort" and Redis is not guaranteed to
work as well as in Linux, OSX, and *BSD there.

It is as simple as:

    % make

You can run a 32 bit Redis binary using:

    % make 32bit

After building Redis is a good idea to test it, using:

    % make test

Fixing build problems with dependencies or cached build options
¡ª--------
Redis has some dependencies which are included into the "deps" directory.
"make" does not rebuild dependencies automatically, even if something in the
source code of dependencies is changes.

When you update the source code with `git pull` or when code inside the
dependencies tree is modified in any other way, make sure to use the following
command in order to really clean everything and rebuild from scratch:

    make distclean

This will clean: jemalloc, lua, hiredis, linenoise.

Also if you force certain build options like 32bit target, no C compiler
optimizations (for debugging purposes), and other similar build time options,
those options are cached indefinitely until you issue a "make distclean"
command.

Fixing problems building 32 bit binaries
---------

If after building Redis with a 32 bit target you need to rebuild it
with a 64 bit target, or the other way around, you need to perform a
"make distclean" in the root directory of the Redis distribution.

In case of build errors when trying to build a 32 bit binary of Redis, try
the following steps:

* Install the packages libc6-dev-i386 (also try g++-multilib).
* Try using the following command line instead of "make 32bit":

    make CFLAGS="-m32 -march=native" LDFLAGS="-m32"

Allocator
---------

Selecting a non-default memory allocator when building Redis is done by setting
the `MALLOC` environment variable. Redis is compiled and linked against libc
malloc by default, with the exception of jemalloc being the default on Linux
systems. This default was picked because jemalloc has proven to have fewer
fragmentation problems than libc malloc.

To force compiling against libc malloc, use:

    % make MALLOC=libc

To compile against jemalloc on Mac OS X systems, use:

    % make MALLOC=jemalloc

Verbose build
-------------

Redis will build with a user friendly colorized output by default.
If you want to see a more verbose output use the following:

    % make V=1

Running Redis
-------------

To run Redis with the default configuration just type:

    % cd src
    % ./redis-server
    
If you want to provide your redis.conf, you have to run it using an additional
parameter (the path of the configuration file):

    % cd src
    % ./redis-server /path/to/redis.conf

It is possible to alter the Redis configuration passing parameters directly
as options using the command line. Examples:

    % ./redis-server --port 9999 --slaveof 127.0.0.1 6379
    % ./redis-server /etc/redis/6379.conf --loglevel debug

All the options in redis.conf are also supported as options using the command
line, with exactly the same name.

Playing with Redis
------------------

You can use redis-cli to play with Redis. Start a redis-server instance,
then in another terminal try the following:

    % cd src
    % ./redis-cli
    redis> ping
    PONG
    redis> set foo bar
    OK
    redis> get foo
    "bar"
    redis> incr mycounter
    (integer) 1
    redis> incr mycounter
    (integer) 2
    redis> 

You can find the list of all the available commands here:

    http://redis.io/commands

Installing Redis
-----------------

In order to install Redis binaries into /usr/local/bin just use:

    % make install

You can use "make PREFIX=/some/other/directory install" if you wish to use a
different destination.

Make install will just install binaries in your system, but will not configure
init scripts and configuration files in the appropriate place. This is not
needed if you want just to play a bit with Redis, but if you are installing
it the proper way for a production system, we have a script doing this
for Ubuntu and Debian systems:

    % cd utils
    % ./install_server.sh

The script will ask you a few questions and will setup everything you need
to run Redis properly as a background daemon that will start again on
system reboots.

You'll be able to stop and start Redis using the script named
/etc/init.d/redis_<portnumber>, for instance /etc/init.d/redis_6379.

Code contributions
---

Note: by contributing code to the Redis project in any form, including sending
a pull request via Github, a code fragment or patch via private email or
public discussion groups, you agree to release your code under the terms
of the BSD license that you can find in the COPYING file included in the Redis
source distribution.

Please see the CONTRIBUTING file in this source distribution for more
information.

Enjoy!
