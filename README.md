# BOM search plugin

![Master Status](https://github.com/olegzzz/bom-search-maven-plugin/workflows/maven-ci/badge.svg)


Plugin to lookup BOM dependencies in maven central by a dependency group. 

## Motivation

Often times a project of decent size contains multiple dependencies that share same group. MOre often than not those 
dependencies managed via a common version hardcoded into project properties. Sometimes dependency management used. 
In any case, it would greatly simplify dependency management if one single BOM file can be referenced to make sure all 
related dependencies have specific version. 

Here is a problem: with a lot of dependencies in the pom-file it's hard to figure out if any two or more share same 
group or if BOM available for those groups. 

This plugin tries to do just that: given the project pom to lookup possible BOM artifacts for a group of dependencies.

## Goals

`search ` runs a search for available BOM artifacts for current project.

### Parameters
- `minOccurrence` minimal number of dependencies that share a group to search for BOM for that group. 
Default is `2`.
- `mavenRepoUrl` maven repository URL. Default is: `https://repo.maven.apache.org/maven2`


### Usage

Typical use of the plugin would look as follows:
```
$ mvn bom-search:search
```
then looking into the log one can decide whether to use suggested Bom dependencies:
```
[INFO] --- bom-search-maven-plugin:<version>:search (default-cli) @ <module> ---
[INFO] Following BOMs found for module: [io.dropwizard:dropwizard-bom]
```

