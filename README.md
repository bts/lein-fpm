# lein-fpm

A Leiningen plugin for building simple packages using
[fpm](https://github.com/jordansissel/fpm).

Generated packages install a standalone JAR for your project in `/usr/lib`, a
wrapper shell script in `/usr/bin`, and an upstart script in `/etc/init`.
lein-fpm assumes that your project can successfully build a functioning
standalone jar via `lein uberjar`.

[![Clojars Project](http://clojars.org/lein-fpm/latest-version.svg)](http://clojars.org/lein-fpm)

## Usage

### System-wide install

Put `[lein-fpm "0.2.3"]` into the `:plugins` vector of your `:user` profile.

### Per-project install

Put `[lein-fpm "0.2.3"]` into the `:plugins` vector of your project.clj.

### Building a package

lein-fpm will produce a deb by default:

    $ lein fpm

or you can supply a specific target type:

    $ lein fpm rpm

This will produce a package in the `target` directory.

#### Included dependencies

By default, deb packages will depend on `openjdk-7-jre-headless`, rpm packages
will depend on `java-1.7.0-openjdk`, and solaris packages will depend on
`jdk-7`.

### Using a package

Install the package using the appropriate package manager, then start the application with upstart:

```bash
$ sudo start APP-NAME
APP-NAME start/running, process 27699
$ status APP-NAME
APP-NAME start/running, process 27699
$ sudo restart APP-NAME
APP-NAME start/running, process 27743
$ sudo stop APP-NAME
APP-NAME stop/waiting
```

## Caveats

At the moment, lein-fpm is quite simple and does not yet support configuration
beyond the target type. Contributions and feedback are welcome! This project is
a bit of an experimental tool I created in trying to produce the simplest
packages that will usefully run within [immutable
servers](http://martinfowler.com/bliki/ImmutableServer.html).

## Dependencies

This plugin depends on [fpm](https://github.com/jordansissel/fpm), and
[rpmbuild](http://www.rpm.org/max-rpm-snapshot/rpmbuild.8.html) if you are
creating rpms.

## License

Copyright © 2015 Brian Schroeder

Distributed under the MIT License.
