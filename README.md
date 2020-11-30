# VACUUM performance testapp

This repository contains a simple instrumentation test which creates a database, initializes it with information, then deletes a subset of the information and runs `VACUUM`.  During the process, statistics about the database file's size are logged to Logcat.

## Building & Running

1. Import the project to Android Studio
1. Connect a Developer-enabled Android Device to your machine
1. Navigate to the `VacuumTest.kt` file and open it within Android Studio
1. From the gutter of the `VacuumTest.kt` editor, click the `run test` icon that appears next to the class header.
