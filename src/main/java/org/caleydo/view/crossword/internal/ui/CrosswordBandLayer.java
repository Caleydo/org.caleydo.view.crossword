/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.view.crossword.internal.ui;

import org.caleydo.core.data.selection.MultiSelectionManagerMixin;
import org.caleydo.core.data.selection.SelectionManager;
import org.caleydo.core.event.EventListenerManager.DeepScan;
import org.caleydo.core.view.opengl.layout2.GLElement;
import org.caleydo.core.view.opengl.layout2.GLGraphics;
import org.caleydo.view.crossword.api.model.BandRoute;
import org.caleydo.view.crossword.api.ui.CrosswordMultiElement;

/**
 * a dedicated layer for the bands for better caching behavior
 *
 * @author Samuel Gratzl
 *
 */
public class CrosswordBandLayer extends GLElement implements MultiSelectionManagerMixin.ISelectionMixinCallback {
	@DeepScan
	private final MultiSelectionManagerMixin selections = new MultiSelectionManagerMixin(this);

	@Override
	protected void renderImpl(GLGraphics g, float w, float h) {
		super.renderImpl(g, w, h);
		for (BandRoute edge : getMultiElement().getBandRoutes()) {
			edge.render(g, w, h, this);
		}
	}

	@Override
	protected void renderPickImpl(GLGraphics g, float w, float h) {
		super.renderImpl(g, w, h);
		for (BandRoute edge : getMultiElement().getBandRoutes()) {
			edge.renderPick(g, w, h, this);
		}
	}

	/**
	 * @return
	 */
	CrosswordMultiElement getMultiElement() {
		return findParent(CrosswordMultiElement.class);
	}

	@Override
	public void onSelectionUpdate(SelectionManager manager) {
		repaint();
	}
}
