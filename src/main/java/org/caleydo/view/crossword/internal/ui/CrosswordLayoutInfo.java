/*******************************************************************************
 * Caleydo - visualization for molecular biology - http://caleydo.org
 *
 * Copyright(C) 2005, 2012 Graz University of Technology, Marc Streit, Alexander
 * Lex, Christian Partl, Johannes Kepler University Linz </p>
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 *******************************************************************************/
package org.caleydo.view.crossword.internal.ui;

import static org.caleydo.view.crossword.internal.Settings.TOOLBAR_WIDTH;
import gleem.linalg.Vec2f;

import java.util.List;

import org.caleydo.core.view.opengl.canvas.IGLMouseListener.IMouseEvent;
import org.caleydo.core.view.opengl.layout2.GLElementAccessor;
import org.caleydo.core.view.opengl.layout2.basic.ScrollingDecorator.IHasMinSize;
import org.caleydo.core.view.opengl.layout2.layout.IGLLayout;
import org.caleydo.core.view.opengl.layout2.layout.IGLLayoutElement;
import org.caleydo.core.view.opengl.layout2.layout.IHasGLLayoutData;
import org.caleydo.core.view.opengl.layout2.manage.GLElementFactorySwitcher.IActiveChangedCallback;
/**
 * layout specific information
 *
 * @author Samuel Gratzl
 *
 */
public class CrosswordLayoutInfo implements IActiveChangedCallback, IGLLayout {
	private final CrosswordElement parent;

	private float zoomFactorX = 1.0f;
	private float zoomFactorY = 1.0f;
	private boolean hovered = false;

	private boolean selected;

	/**
	 * @param crosswordElement
	 */
	public CrosswordLayoutInfo(CrosswordElement parent) {
		this.parent = parent;
	}


	/**
	 * @param zoomFactor
	 *            setter, see {@link zoomFactor}
	 */
	public boolean setZoomFactor(float zoomFactorX, float zoomFactorY) {
		if (this.zoomFactorX == zoomFactorX && this.zoomFactorY == zoomFactorY)
			return false;
		this.zoomFactorX = zoomFactorX;
		this.zoomFactorY = zoomFactorY;
		parent.getParent().relayout();
		return true;
	}

	@Override
	public void onActiveChanged(int active) {
		// reset to a common zoom factor
		float s = Math.min(zoomFactorX, zoomFactorY);
		if (!setZoomFactor(s, s))
			parent.getParent().relayout(); // the min size may have changed
	}

	/**
	 * @return the zoomFactor, see {@link #zoomFactor}
	 */
	public float getZoomFactorX() {
		return zoomFactorX;
	}

	/**
	 * @return the zoomFactorY, see {@link #zoomFactorY}
	 */
	public float getZoomFactorY() {
		return zoomFactorY;
	}

	/**
	 * @param factor
	 */
	public boolean zoom(float factor) {
		if (factor == 1.0f || Double.isNaN(factor) || Double.isInfinite(factor) || factor <= 0)
			return false;
		this.zoomFactorX = zoomFactorX * factor;
		this.zoomFactorY = zoomFactorY * factor;
		parent.getParent().relayout();
		return true;
	}

	/**
	 * zoom implementation of the given picking event
	 *
	 * @param event
	 */
	public void zoom(IMouseEvent event) {
		if (!event.isCtrlDown() || event.getWheelRotation() == 0)
			return;
		float factor = (float) Math.pow(1.2, event.getWheelRotation());
		boolean isCenteredZoom = !event.isShiftDown();
		zoom(factor);
		if (isCenteredZoom) {
			Vec2f pos = parent.toRelative(event.getPoint());
			// compute the new new mouse pos considers zoom
			Vec2f new_ = pos.times(factor);
			pos.sub(new_);
			// shift the location according to the delta
			shift(pos.x(), pos.y());
		}
	}

	/**
	 * shift the location the item
	 *
	 * @param x
	 * @param y
	 */
	public void shift(float x, float y) {
		if (x == 0 && y == 0)
			return;
		Vec2f loc = parent.getLocation();
		parent.setLocation(loc.x() + x, loc.y() + y);
	}

	/**
	 * enlarge the view by moving and rescaling
	 *
	 * @param x
	 *            the dx
	 * @param xDir
	 *            the direction -1 to the left +1 to the right 0 nothing
	 * @param y
	 *            the dy
	 * @param yDir
	 */
	public void enlarge(float x, int xDir, float y, int yDir) {
		Vec2f size = parent.getSize();
		Vec2f loc = parent.getLocation();
		float sx = size.x() + xDir * x;
		float sy = size.y() + yDir * y;
		// convert to scale factor
		sx -= 2; // borders and buttons
		sy -= 2;
		Vec2f minSize = getMinSize(parent);
		setZoomFactor(sx / minSize.x(), sy / minSize.y());
		parent.setLocation(loc.x() + (xDir < 0 ? x : 0), loc.y() + (yDir < 0 ? y : 0));
	}

	/**
	 * @param hovered
	 *            setter, see {@link hovered}
	 */
	public void setHovered(boolean hovered) {
		if (this.hovered == hovered)
			return;
		this.hovered = hovered;
		parent.relayout();
	}

	/**
	 * @param b
	 */
	public void setSelected(boolean selected) {
		if (this.selected == selected)
			return;
		this.selected = selected;
		repaintToolBars();
	}

	/**
	 * @return the selected, see {@link #selected}
	 */
	public boolean isSelected() {
		return selected;
	}

	/**
	 * @return the hovered, see {@link #hovered}
	 */
	public boolean isHovered() {
		return hovered;
	}

	@Override
	public void doLayout(List<? extends IGLLayoutElement> children, float w, float h) {
		IGLLayoutElement content = children.get(0);
		IGLLayoutElement border = children.get(1);
		IGLLayoutElement header = children.get(2);
		IGLLayoutElement toolbar = children.get(3);
		final float shift = 1;
		final float shift2 = shift + shift;
		content.setBounds(shift, shift, w - shift2, h - shift2);

		final int tw = TOOLBAR_WIDTH + 2;
		if (hovered || parent.getMultiElement().isAlwaysShowHeader())
			header.setBounds(-shift, -shift2 - tw, w + shift2, tw);
		else
			header.setBounds(-shift, -shift, w + shift2, 0);
		if (hovered)
			toolbar.setBounds(-shift - tw, -shift, tw, h + shift2);
		else
			toolbar.setBounds(-shift, -shift, 0, h + shift2);

		border.setBounds(0, 0, w, h);
	}

	/**
	 *
	 */
	private void repaintToolBars() {
		GLElementAccessor.repaintDown(parent.get(2));
		GLElementAccessor.repaintDown(parent.get(3));
	}

	void scale(Vec2f size) {
		size.setX(size.x() * zoomFactorX);
		size.setY(size.y() * zoomFactorY);
		size.setX(size.x() + 2);
		size.setY(size.y() + 2); // for buttons and border
	}

	Vec2f getLocation(IGLLayoutElement elem) {
		return elem.getSetLocation().copy();
	}

	Vec2f getMinSize(IHasGLLayoutData elem) {
		IHasMinSize minSize = elem.getLayoutDataAs(IHasMinSize.class, null);
		if (minSize != null)
			return minSize.getMinSize();
		return elem.getLayoutDataAs(Vec2f.class, new Vec2f(100, 100));
	}

	void setBounds(IGLLayoutElement elem, Vec2f loc, Vec2f size) {
		elem.setBounds(loc.x(), loc.y(), size.x(), size.y());
	}
}