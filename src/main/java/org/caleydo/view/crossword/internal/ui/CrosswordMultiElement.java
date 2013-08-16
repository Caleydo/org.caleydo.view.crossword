/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 ******************************************************************************/
package org.caleydo.view.crossword.internal.ui;

import gleem.linalg.Vec2f;
import gleem.linalg.Vec4f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.caleydo.core.data.collection.table.Table;
import org.caleydo.core.data.datadomain.ATableBasedDataDomain;
import org.caleydo.core.data.perspective.table.TablePerspective;
import org.caleydo.core.data.perspective.variable.Perspective;
import org.caleydo.core.data.virtualarray.group.Group;
import org.caleydo.core.data.virtualarray.group.GroupList;
import org.caleydo.core.id.IDType;
import org.caleydo.core.view.opengl.layout2.GLElement;
import org.caleydo.core.view.opengl.layout2.GLElementAccessor;
import org.caleydo.core.view.opengl.layout2.GLGraphics;
import org.caleydo.core.view.opengl.layout2.IGLElementContext;
import org.caleydo.core.view.opengl.layout2.IGLElementParent;
import org.caleydo.core.view.opengl.layout2.basic.ScrollingDecorator.IHasMinSize;
import org.caleydo.core.view.opengl.layout2.layout.IGLLayoutElement;
import org.caleydo.view.crossword.internal.CrosswordView;
import org.caleydo.view.crossword.internal.model.PerspectiveMetaData;
import org.caleydo.view.crossword.internal.model.TablePerspectiveMetaData;
import org.caleydo.view.crossword.internal.ui.band.ABandEdge;
import org.caleydo.view.crossword.internal.ui.band.ParentChildBandEdge;
import org.caleydo.view.crossword.internal.ui.band.SharedBandEdge;
import org.caleydo.view.crossword.internal.ui.band.SiblingBandEdge;
import org.caleydo.view.crossword.internal.ui.layout.DefaultGraphLayout;
import org.caleydo.view.crossword.internal.ui.layout.IGraphEdge;
import org.caleydo.view.crossword.internal.ui.layout.IGraphLayout;
import org.caleydo.view.crossword.internal.ui.layout.IGraphVertex;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.Pseudograph;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

/**
 * layout implementation
 *
 * @author Samuel Gratzl
 *
 */
public class CrosswordMultiElement extends GLElement implements IHasMinSize, IGLElementParent,
		Iterable<CrosswordElement> {

	private final UndirectedGraph<CrosswordElement, ABandEdge> graph = new Pseudograph<>(ABandEdge.class);
	private final CrosswordBandLayer bands = new CrosswordBandLayer();

	private final IGraphLayout layout = new DefaultGraphLayout();

	private boolean alwaysShowHeader;

	/**
	 *
	 */
	public CrosswordMultiElement() {
		GLElementAccessor.setParent(bands, this);
	}

	@Override
	public Vec2f getMinSize() {
		float x = 0;
		float y = 0;
		for (GLElement child : this) {
			Vec4f bounds = child.getBounds();
			x = Math.max(x, bounds.x() + bounds.z());
			y = Math.max(y, bounds.y() + bounds.w());
		}
		return new Vec2f(x, y);
	}

	/**
	 * @return the alwaysShowHeader, see {@link #alwaysShowHeader}
	 */
	public boolean isAlwaysShowHeader() {
		return alwaysShowHeader;
	}

	/**
	 *
	 */
	public void toggleAlwaysShowHeader() {
		this.alwaysShowHeader = !this.alwaysShowHeader;
		for (CrosswordElement elem : Iterables.filter(this, CrosswordElement.class))
			elem.relayout();
	}

	/**
	 * @param dimensionSubTablePerspectives
	 */
	public void add(TablePerspective tablePerspective) {
		add(new CrosswordElement(tablePerspective, new TablePerspectiveMetaData(0, 0)));
	}

	@Override
	public void layout(int deltaTimeMs) {
		super.layout(deltaTimeMs);
		bands.layout(deltaTimeMs);
		for (GLElement child : this)
			child.layout(deltaTimeMs);
	}

	@Override
	public void relayout() {
		super.relayout();
		GLElementAccessor.relayoutDown(bands);
	}

	@Override
	protected final void layoutImpl() {
		super.layoutImpl();
		Vec2f size = getSize();
		GLElementAccessor.asLayoutElement(bands).setBounds(0, 0, size.x(), size.y());

		layout.doLayout(asGraphVertices(), graph.edgeSet());
		relayoutParent(); // trigger update of the parent for min size changes
	}

	private List<IGraphVertex> asGraphVertices() {
		Set<CrosswordElement> s = graph.vertexSet();
		List<IGraphVertex> l = new ArrayList<>(s.size());
		for (CrosswordElement child : s)
			if (child.getVisibility() != EVisibility.NONE)
				l.add(new GraphVertex(child));
		return l;
	}

	@Override
	protected void takeDown() {
		GLElementAccessor.takeDown(bands);
		for (GLElement elem : this)
			GLElementAccessor.takeDown(elem);
		super.takeDown();
	}

	@Override
	protected void init(IGLElementContext context) {
		super.init(context);
		GLElementAccessor.init(bands, context);
		for (GLElement child : this)
			GLElementAccessor.init(child, context);
	}

	private void setup(CrosswordElement child) {
		IGLElementParent ex = child.getParent();
		boolean doInit = ex == null;
		if (ex == this) {
			// internal move
			graph.removeVertex(child);
		} else if (ex != null) {
			doInit = !ex.moved(child);
		}
		GLElementAccessor.setParent(child, this);
		if (doInit && context != null)
			GLElementAccessor.init(child, context);
	}

	/**
	 * @param crosswordElement
	 */
	private void add(CrosswordElement child) {
		addImpl(child);
		createBands(child, ImmutableSet.of(child));
	}

	private void addImpl(CrosswordElement child) {
		setup(child);
		graph.addVertex(child);
		relayout();
	}

	private void createBands(CrosswordElement child, Set<CrosswordElement> ignores) {
		final TablePerspective tablePerspective = child.getTablePerspective();
		final IDType recIDType = tablePerspective.getRecordPerspective().getIdType();
		final IDType dimIDType = tablePerspective.getDimensionPerspective().getIdType();

		for (CrosswordElement other : graph.vertexSet()) {
			if (ignores.contains(other))
				continue;
			final TablePerspective tablePerspective2 = child.getTablePerspective();
			final IDType recIDType2 = tablePerspective2.getRecordPerspective().getIdType();
			final IDType dimIDType2 = tablePerspective2.getDimensionPerspective().getIdType();
			if (recIDType.resolvesTo(recIDType2))
				addEdgeImpl(child, other, new SharedBandEdge(false, false));
			if (dimIDType.resolvesTo(recIDType2))
				addEdgeImpl(child, other, new SharedBandEdge(true, false));
			if (recIDType.resolvesTo(dimIDType2))
				addEdgeImpl(child, other, new SharedBandEdge(false, true));
			if (dimIDType.resolvesTo(dimIDType2))
				addEdgeImpl(child, other, new SharedBandEdge(true, true));
		}
	}

	private void addEdgeImpl(CrosswordElement child, CrosswordElement other, final ABandEdge edge) {
		graph.addEdge(child, other, edge);
		edge.update();
	}

	/**
	 * @param crosswordElement
	 * @param dimensionSubTablePerspectives
	 */
	private void split(CrosswordElement base, boolean inDim) {
		TablePerspective table = base.getTablePerspective();
		boolean hor = inDim;
		final GroupList groups = (inDim ? table.getDimensionPerspective() : table.getRecordPerspective())
				.getVirtualArray().getGroupList();
		assert groups.size() > 1;
		final List<TablePerspective> datas = inDim ? table.getDimensionSubTablePerspectives() : table
				.getRecordSubTablePerspectives();
		List<CrosswordElement> children = new ArrayList<>(datas.size());
		{
			TablePerspectiveMetaData metaData = new TablePerspectiveMetaData(
					inDim ? 0 : PerspectiveMetaData.FLAG_CHILD, inDim ? PerspectiveMetaData.FLAG_CHILD : 0);
			for (TablePerspective t : datas) {
				final CrosswordElement new_ = new CrosswordElement(t, metaData);
				new_.initFromParent(base);
				children.add(new_);
			}
		}

		// combine the elements that should be ignored
		ImmutableSet<CrosswordElement> ignore = ImmutableSet.<CrosswordElement> builder().addAll(children).add(base)
				.build();

		for (int i = 0; i < children.size(); ++i) {
			CrosswordElement child = children.get(i);
			Group group = groups.get(i);
			addImpl(child);
			createBands(child, ignore);
			addEdgeImpl(base, child, new ParentChildBandEdge(hor, group.getStartIndex(), hor)); // add parent edge
			for (int j = 0; j < i; ++j) {
				CrosswordElement child2 = children.get(j);
				addEdgeImpl(child, child2, new SiblingBandEdge(hor, hor)); // add sibling edge
			}
		}

		// update metadata flags
		TablePerspectiveMetaData metaData = base.getMetaData();
		(inDim ? metaData.getDimension() : metaData.getRecord()).setSplitted();
	}

	/**
	 * @param crosswordElement
	 */
	public void splitDim(CrosswordElement child) {
		split(child, true);
	}

	public void splitRec(CrosswordElement child) {
		split(child, false);
	}

	@Override
	public boolean moved(GLElement child) {
		if (child instanceof CrosswordElement)
			graph.removeVertex((CrosswordElement) child);
		relayout();
		return context != null;
	}

	@Override
	public Iterator<CrosswordElement> iterator() {
		return Iterators.unmodifiableIterator(graph.vertexSet().iterator());
	}

	@Override
	protected void renderImpl(GLGraphics g, float w, float h) {
		super.renderImpl(g, w, h);
		bands.render(g);
		g.incZ();
		for (GLElement child : graph.vertexSet())
			child.render(g);
		g.decZ();
	}

	@Override
	protected void renderPickImpl(GLGraphics g, float w, float h) {
		super.renderPickImpl(g, w, h);
		bands.renderPick(g);
		g.incZ();
		for (GLElement child : graph.vertexSet())
			child.renderPick(g);
		g.decZ();
	}

	@Override
	protected boolean hasPickAbles() {
		return super.hasPickAbles() || !graph.vertexSet().isEmpty();
	}

	/**
	 * @param r
	 */
	void remove(CrosswordElement child) {
		if (graph.removeVertex(child)) {
			GLElementAccessor.takeDown(child);
			relayout();
		}
	}

	public void addAll(Iterable<TablePerspective> tablePerspectives) {
		for (TablePerspective tablePerspective : tablePerspectives)
			add(tablePerspective);
	}

	/**
	 * @param removed
	 */
	public void removeAll(Collection<TablePerspective> removed) {
		if (removed.isEmpty())
			return;
		List<CrosswordElement> toRemove = new ArrayList<>();
		for (CrosswordElement elem : Iterables.filter(this, CrosswordElement.class)) {
			if (removed.contains(elem.getTablePerspective())
					|| removed.contains(elem.getTablePerspective().getParentTablePerspective()))
				toRemove.add(elem);
		}
		for (CrosswordElement r : toRemove)
			remove(r);
	}

	public Iterable<ABandEdge> getBands() {
		return Iterables.unmodifiableIterable(graph.edgeSet());
	}

	/**
	 * @param crosswordElement
	 */
	public void onConnectionsChanged(CrosswordElement child) {
		for (ABandEdge edge : graph.edgesOf(child))
			edge.update();
	}

	public void changePerspective(CrosswordElement child, boolean isDim, Perspective new_) {
		TablePerspective old = child.getTablePerspective();
		ATableBasedDataDomain dataDomain = old.getDataDomain();

		new_ = dataDomain.convertForeignPerspective(new_);
		Table table = dataDomain.getTable();

		Perspective record = old.getRecordPerspective();
		Perspective dimension = old.getDimensionPerspective();
		if (isDim)
			dimension = new_;
		else
			record = new_;
		TablePerspective newT;
		if (!table.containsDimensionPerspective(dimension.getPerspectiveID())
				|| !table.containsRecordPerspective(record.getPerspectiveID())) {
			newT = new TablePerspective(dataDomain, record, dimension);
			newT.setPrivate(true);
		} else
			newT = dataDomain.getTablePerspective(record.getPerspectiveID(), dimension.getPerspectiveID());
		child.setTablePerspective(newT);
		if (context instanceof CrosswordView) {
			((CrosswordView) context).replaceTablePerspectiveInternally(old, newT);
		}
	}

	private class GraphVertex implements IGraphVertex {
		private final IGLLayoutElement elem;
		private final CrosswordLayoutInfo info;

		public GraphVertex(CrosswordElement elem) {
			this.elem = GLElementAccessor.asLayoutElement(elem);
			this.info = elem.getLayoutDataAs(CrosswordLayoutInfo.class, null);
		}

		@Override
		public Vec2f getLocation() {
			return elem.getLocation();
		}

		/**
		 * @return
		 */
		@Override
		public Vec2f getSize() {
			Vec2f size = info.getMinSize(elem);
			info.scale(size);
			return size;
		}


		@Override
		public void setBounds(Vec2f location, Vec2f size) {
			elem.setBounds(location.x(), location.y(), size.x(), size.y());
		}

		@Override
		public void move(float x, float y) {
			Vec2f l = elem.getLocation();
			elem.setLocation(l.x() + x, l.y() + y);
		}

		@Override
		public Set<? extends IGraphEdge> getEdges() {
			return graph.edgesOf((CrosswordElement) elem.asElement());
		}

	}
}
