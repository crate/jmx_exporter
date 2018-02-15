===============
Developer Guide
===============

Building
========

This project uses Gradle_ as build tool.

Gradle can be invoked like so::

  $ ./gradlew

The first time this command is executed, Gradle is downloaded and bootstrapped
for you automatically.

Building the JAR
================

Run::

  $ ./gradlew buildJar

Testing
=======

Run the unit and integration tests like so::

  $ ./gradlew test

.. _Gradle: https://gradle.org/
