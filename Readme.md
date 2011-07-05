Rovio Joystick Console
==========

This is a Java and Groovy UI that controls a Rovio Robot with a
Sidewinder Joystick. Pretty much nay 3 axis Joystick should work.

It incorporates the recent Video brightness improvements by 
[Gandalf](http://www.robocommunity.com/forum/thread/17515/Fix-of-the-Rovio-brightness-control)

It has a video window so you can see where you are going.

The Joystick control uses the z-axis to rotate, the x-axis to move
sideways with proportional speed control and the y-axis to move
forward and back. It is a very intuitive way to drive a Rovio.

The two side buttons on the Sidewinder move the Camera head up and
down, short presses move it up a little so it can have more than three
positions.

All the library dependencies are in the ./libs directory.

The UI uses SwingBuilder from Groovy for the UI, which makes it pretty
easy to write and modify.

Currently it can to be built with eclipse and the groovy/eclipse plugin.
Also buildr will build it.

The admin login for the Rovio can be entered on the command line as
hostname username password, or left blank and the first time the program runs
it will ask for the login and store them in the Java default
preferences location.

Eclipse setup
------------

Under eclipse you need to add -Djava.library.path=./libs to the
Run configuration or use the RovioConsole.launch which has it already
setup.

Also in the Preferences/Groovy/Compiler check the Enable script folder support and add
src/main/resources/**/*.groovy to the exclude list if not already there.

Buildr
-----

You can also build from the command line using
[buildr](http://buildr.apache.org/)

Presuming you have a ruby in your path...

> gem install buildr

then

> buildr test=no package

then it can be run with

> run

or 

> run hostOrIP username password

or

> buildr test=no run

To set a username and password for the buildr run you can create a
file in the project directory called `user.local.yml` and put the
following in it..

    rovio:
      host: ip or hostname
      username: myusername
      password: mypassword


Currently only tested on Linux, however there is a joystick dll in the
libs directory, so it should work on windows if the dll is on the
PATH.

Once running click the start button and the video window should
appear, then the AGC or Night mode can be set or the resolution from
the UI, the rest is controlled from the joystick.

The Status bar at the bottom shows the frames per second we are
getting via MJPEG, what we are connected to, and the approximate
battery level.

Copyright Jim Morris 2011
Licensed under the Apache license
http://www.apache.org/licenses/LICENSE-2.0.html

The Java Joystick library is Copyright 2000-2001 George Rhoten and others.
Available from http://sourceforge.net/projects/javajoystick/
