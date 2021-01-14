# BOM search maven plugin

![Master Status](https://github.com/olegzzz/bom-search-maven-plugin/workflows/maven-ci/badge.svg)
![CodeQL](https://github.com/olegzzz/bom-search-maven-plugin/workflows/CodeQL/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.olegzzz/bom-search-maven-plugin)](https://maven-badges.herokuapp.com/maven-central/com.github.olegzzz/bom-search-maven-plugin)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)


Plugin to lookup bill of materials (BOM) artifacts to retrofit existing project.

## Motivation

Often times a project of decent size contains multiple dependencies that share same group. More often than not those 
dependencies managed via a common version hardcoded into project properties. Sometimes dependency management used. 
In any case, it would greatly simplify dependency management if one single BOM file can be referenced to make sure all 
related dependencies have specific version. 

Here is a problem: with a lot of dependencies in the pom-file it's hard to figure out if any two or more share same 
group or if BOM available for those groups. 

This plugin tries to do just that: given the project pom to lookup possible BOM artifacts for a group of dependencies.

## Goals

`search ` runs a search for available BOM artifacts for current project.

### Configuration

Parameter | Type | Description | Default
----------|------|---------|------------
minOccurrence | int | Minimal number of dependencies that share a group to search for BOM for that group | 2
mavenRepoUrl | URL | Maven repository URL to search artifact | https://repo.maven.apache.org/maven2
incremental | boolean | Whether to store results for a re-use | true

### Usage

Typical use of the plugin would look as follows:
```
$ mvn com.github.olegzzz:bom-search-maven-plugin:search
```
let's say a project has multiple dropwizard dependencies, then looking into the log one can pick suggested BOM file (`io.dropwizard:dropwizard-bom` in this case):
```
[INFO] --- bom-search-maven-plugin:1.2:search (default-cli) ---
[INFO] Following BOMs found for module: [io.dropwizard:dropwizard-bom]
```

