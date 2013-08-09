/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 ******************************************************************************/
package org.caleydo.view.crossword.internal;
import java.util.List;

import org.caleydo.core.data.datadomain.DataSupportDefinitions;
import org.caleydo.core.data.datadomain.IDataSupportDefinition;
import org.caleydo.core.data.perspective.table.TablePerspective;
import org.caleydo.core.event.EventListenerManager.ListenTo;
import org.caleydo.core.serialize.ASerializedView;
import org.caleydo.core.view.opengl.canvas.IGLCanvas;
import org.caleydo.core.view.opengl.layout2.GLElement;
import org.caleydo.core.view.opengl.layout2.GLElementDecorator;
import org.caleydo.core.view.opengl.layout2.basic.ScrollingDecorator;
import org.caleydo.core.view.opengl.layout2.view.AMultiTablePerspectiveElementView;
import org.caleydo.view.crossword.internal.event.ToggleHeaderAlwaysEvent;
import org.caleydo.view.crossword.internal.serial.SerializedCrosswordView;
import org.caleydo.view.crossword.internal.ui.CrosswordMultiElement;

/**
 * basic view based on {@link GLElement} with a {@link AMultiTablePerspectiveElementView}
 *
 * @author Samuel Gratzl
 *
 */
public class GLCrosswordView extends AMultiTablePerspectiveElementView {
	public static final String VIEW_TYPE = "org.caleydo.view.crossword";
	public static final String VIEW_NAME = "CrossWord";

	private final CrosswordMultiElement crossword;

	public GLCrosswordView(IGLCanvas glCanvas) {
		super(glCanvas, VIEW_TYPE, VIEW_NAME);
		this.crossword = new CrosswordMultiElement();
	}

	@Override
	public ASerializedView getSerializableRepresentation() {
		SerializedCrosswordView serializedForm = new SerializedCrosswordView(this);
		return serializedForm;
	}

	@Override
	protected GLElement createContent() {
		return ScrollingDecorator.wrap(crossword, Settings.SCROLLBAR_WIDTH);
	}

	@Override
	public IDataSupportDefinition getDataSupportDefinition() {
		return DataSupportDefinitions.all;
	}

	@Override
	protected void applyTablePerspectives(GLElementDecorator root, List<TablePerspective> all,
			List<TablePerspective> added, List<TablePerspective> removed) {
		crossword.removeAll(removed);
		crossword.addAll(added);

	}

	@ListenTo(sendToMe = true)
	private void onToggleHeaderAlwaysHeader(ToggleHeaderAlwaysEvent event) {
		crossword.toggleAlwaysShowHeader();
	}
}