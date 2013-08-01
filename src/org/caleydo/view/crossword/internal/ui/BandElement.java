/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.crossword.internal.ui;

import java.util.Set;

/**
 * @author Samuel Gratzl
 *
 */
public class BandElement {
	private final CrosswordElement left;
	private final CrosswordElement right;
	private Set<Integer> shared;
}
