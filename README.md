# clojure-ipp

A pure Clojure implementation of the server-side of the Internet Printing Protocol 2.0.

The main purpose of this library is the creation of virtual network
printers with a Clojure backend. This is useful to allow clients to
print to a Clojure application where the data (usually Postscript or
PDF) is handled further.

## Usage

For the most use cases, `ipp.server` implements a basic server.

`ipp.test_instance` is a standalone test instance example.
Run `lein repl` and `(start)` to launch it.
It prints the first 256 bytes of the files it receives to stdout.

## Implementation

The actual IPP logic is in `ipp.operations`. Only the bare minimum
needed to obtain printed files is implemented at this moment.

`ipp.parser` and `ipp.serializer` implement the subset of IPP format
that is actually used by `ipp.operations`. Nevertheless, the support
is complete in the sense that unknown and unhandled data types are
still passed around as blobs and survive parse+serialize roundtrips.

## License

Copyright © 2016 Evgeny Egorochkin

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

