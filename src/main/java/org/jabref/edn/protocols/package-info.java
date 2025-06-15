/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
/**
 * This package provides the basis for implementing the printer.
 *
 * <p>A protocol is a function (method) of which implementations can
 * be provided for any number of types.
 *
 * <p>What this package provides is not quite a protocol, but
 * something a bit more general: a kind of Map which looks up values
 * by keys. The values can be of any type, but the keys must be of
 * type {@link java.lang.Class}. The trick here is that lookup takes
 * inheritance into account such that we don't need to specify an
 * implementation for every concrete type we expect to encounter.
 */
package org.jabref.edn.protocols;
