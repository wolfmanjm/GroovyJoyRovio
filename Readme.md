Rovio Joystick Console
==========

This is a Java and Groovy UI that controls a Rovio Robot with a
Sidewinder Joystick.

It incorporates the recent VIdeo brightness improvements by 
[Gandalf](http://www.robocommunity.com/forum/thread/17515/Fix-of-the-Rovio-brightness-control)

It has a video window so you can see where you are going.

The Joystick control uses the rotation to rotate, the backwards and
forward to move straight with proportional speed control and
left/right also with speed control.

It uses SwingBuilder from Groovy for the UI.

Currently it can to be built with eclipse and the groovy/eclipse plugin.
Also buildr will build it.

presuming you have a ruby in your path...
> gem install buildr

then

> buildr test=no package

then it can be run with the run script.

(Currently only tested on Linux)
