<!--

    Sonatype Nexus (TM) Open Source Version
    Copyright (c) 2018-present Sonatype, Inc.
    All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.

    This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
    which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.

    Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
    of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
    Eclipse Foundation. All other trademarks are the property of their respective owners.

-->
## vgo Repositories

### Introduction

[vgo](https://github.com/golang/go/wiki/vgo) is a package manager used for golang, which allows you to manage dependencies 
for your golang projects.

### Installing vgo

You will need to ensure you have golang 1.11 or higher installed.

Go Module support is still experimental, and needs to be explicitly turned on. You can do this by setting
the following environment variable like such:

```
GO111MODULE=on
```

### Proxying vgo Repositories

You can set up an vgo proxy repository to access a remote repository location, for example to proxy packages through 
the [Athens Project](https://athens.azurefd.net/).

To proxy a vgo packages, you simply create a new 'vgo (proxy)' as documented in 
[Repository Management](https://help.sonatype.com/display/NXRM3/Configuration#Configuration-RepositoryManagement) in
details. Minimal configuration steps are:

- Define 'Name'
- Define URL for 'Remote storage' e.g. [https://athens.azurefd.net/](https://athens.azurefd.net/)
- Select a 'Blob store' for 'Storage'

### Configuring vgo 

Configuring vgo to use Nexus Repository is fairly easy! Once you have golang and vgo up and running you'll need 
to add an environment variable similar to the following:

```
export GOPROXY=http://localhost:8081/repository/vgo-proxy/
```

### Browsing vgo Repository Packages

You can browse vgo repositories in the user interface inspecting the components and assets and their details, as
described in [Browsing Repositories and Repository Groups](https://help.sonatype.com/display/NXRM3/Browsing+Repositories+and+Repository+Groups).
