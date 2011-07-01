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

You will need to edit the USERERNAME and PASSWORD variables to match,
or enter them as arguments on the comamnd line or run configuration.

Also under eclipse you need to add -Djava.library.path=./libs to the
Run configuration as this is not saved locally.

presuming you have a ruby in your path...
> gem install buildr

then

> buildr test=no package

then it can be run with the 

> run username password

script or with 

> buildr test=no run

Currently only tested on Linux, however there is a joystick dll in the
libs directory, so it should work on windows if the dll is on the
PATH.

Licensed under the Apache license
http://www.apache.org/licenses/LICENSE-2.0.html
