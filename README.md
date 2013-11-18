# lein-fpm

A Leiningen plugin for generating minimalist packages using
[fpm](https://github.com/jordansissel/fpm).

Generated packages install a standalone jar for your project in `/usr/lib`, and
a wrapper shell script in `/usr/bin` named after your project. lein-fpm assumes
that your project can successfully build a functioning standalone jar via `lein
uberjar`.

## Usage

### System-wide install

Put `[lein-fpm "0.1.0-SNAPSHOT"]` into the `:plugins` vector of your `:user`
profile, or if you are on Leiningen 1.x do `lein plugin install lein-fpm
0.1.0-SNAPSHOT`.

### Per-project install

Put `[lein-fpm "0.1.0-SNAPSHOT"]` into the `:plugins` vector of your
project.clj.

### Running

lein-fpm will produce a deb by default:

    $ lein fpm

or you can pass a specific target:

    $ lein fpm rpm

This will produce a package in the `target` directory.

#### Included dependencies

By default, deb packages will depend on `openjdk-7-jre`, rpm packages will
depend on `java-1.7.0-openjdk`, and solaris packages will depend on `jdk-7`.

## Caveats

At the moment, lein-fpm is quite simple and does not yet support configuration
beyond the target type. Contributions are welcome!

## Dependencies

This plugin depends on [fpm](https://github.com/jordansissel/fpm), and
[rpmbuild](http://www.rpm.org/max-rpm-snapshot/rpmbuild.8.html) if you are
creating rpms.

## License

Copyright © 2013 Brian Schroeder

Distributed under the MIT License.
