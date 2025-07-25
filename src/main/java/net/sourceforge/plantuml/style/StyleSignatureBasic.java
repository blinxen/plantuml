/* ========================================================================
 * PlantUML : a free UML diagram generator
 * ========================================================================
 *
 * (C) Copyright 2009-2024, Arnaud Roques
 *
 * Project Info:  https://plantuml.com
 * 
 * If you like this project or if you find it useful, you can support us at:
 * 
 * https://plantuml.com/patreon (only 1$ per month!)
 * https://plantuml.com/paypal
 * 
 * This file is part of PlantUML.
 *
 * PlantUML is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlantUML distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
 * License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 *
 * Original Author:  Arnaud Roques
 *
 *
 */
package net.sourceforge.plantuml.style;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.sourceforge.plantuml.stereo.Stereostyles;
import net.sourceforge.plantuml.stereo.Stereotype;
import net.sourceforge.plantuml.stereo.StereotypeDecoration;
import net.sourceforge.plantuml.text.Guillemet;
import net.sourceforge.plantuml.url.Url;

public class StyleSignatureBasic implements StyleSignature {
	// ::remove file when __HAXE__

	public static final String STAR = "*";
	private final Set<String> names = new LinkedHashSet<>();
	private final boolean withDot;

	public StyleSignatureBasic(String s) {
		if (s.contains(STAR) || s.contains("&") || s.contains("-"))
			throw new IllegalArgumentException();

		this.withDot = s.contains(".");
		this.names.add(clean(s));
	}

	public static StyleSignatureBasic empty() {
		return new StyleSignatureBasic(false);
	}

	private StyleSignatureBasic(boolean withDot) {
		this.withDot = withDot;
	}

	private StyleSignatureBasic(boolean withDot, Collection<String> copy) {
		this.names.addAll(copy);
		this.withDot = withDot;
	}

	public StyleSignatureBasic addClickable(Url url) {
		if (url == null)
			return this;

		final Set<String> result = new LinkedHashSet<>(names);
		result.add(SName.clickable.name());
		return new StyleSignatureBasic(withDot, result);

	}

	public StyleSignatureBasic add(String s) {
		if (s == null)
			return this;

		if (s.contains("&"))
			throw new IllegalArgumentException();

		final Set<String> result = new LinkedHashSet<>(names);
		result.add(clean(s));
		return new StyleSignatureBasic(withDot || s.contains("."), result);
	}

	public StyleSignatureBasic addS(Stereotype stereo) {
		if (stereo == null)
			return this;

		final Set<String> result = new LinkedHashSet<>(names);
		boolean withDotLocal = withDot;
		for (String s : stereo.getLabels(Guillemet.NONE)) {
			if (s.contains("&"))
				throw new IllegalArgumentException();
			result.add(StereotypeDecoration.PREFIX + clean(s));
			withDotLocal = withDotLocal || s.contains(".");
		}

		return new StyleSignatureBasic(withDotLocal, result);
	}

	public StyleSignatureBasic add(SName name) {
		return add(name.name().toLowerCase().replace("_", ""));
	}

	public StyleSignatureBasic addStar() {
		final Set<String> result = new LinkedHashSet<>(names);
		result.add(STAR);
		return new StyleSignatureBasic(withDot, result);
	}

	public boolean isStarred() {
		return names.contains(STAR);
	}

	@Override
	public boolean equals(Object arg) {
		final StyleSignatureBasic other = (StyleSignatureBasic) arg;
		return this.names.equals(other.names);
	}

	private final AtomicInteger cachedHashCode = new AtomicInteger(0);

	@Override
	public int hashCode() {
		int hash = cachedHashCode.get();
		if (hash == 0) {
			hash = names.hashCode();
			cachedHashCode.set(hash);
		}
		return hash;
	}

	@Override
	public String toString() {
		final StringBuilder result = new StringBuilder();
		for (String n : names) {
			if (result.length() > 0)
				result.append('.');

			result.append(n);
		}
		return result.toString() + " " + withDot;
	}

	private final AtomicInteger cachedDepth = new AtomicInteger(Integer.MIN_VALUE);

	private int depthFromTokens() {
		final int result = cachedDepth.get();
		if (result != Integer.MIN_VALUE)
			return result;

		for (String token : names) {
			final int depth = depthFromSingleToken(token);
			if (depth != -1) {
				cachedDepth.set(depth);
				return depth;
			}

		}
		cachedDepth.set(-1);
		return -1;
	}

	private final Map<StyleSignatureBasic, Boolean> matchAllCache = new ConcurrentHashMap<>();

	public boolean matchAll(StyleSignatureBasic other) {
		final Boolean computeIfAbsent = matchAllCache.computeIfAbsent(other, k -> this.matchAllImpl(other));
		return computeIfAbsent;
	}

	private boolean matchAllImpl(StyleSignatureBasic other) {
		final boolean namesContainsStar = names.contains(STAR);
		if (other.isStarred() && namesContainsStar == false)
			return false;

		final int depthInNames = other.depthFromTokens();

		for (String token : names) {
			if (token.equals(STAR))
				continue;

			int tokenDepth = -1;
			if (namesContainsStar && depthInNames != -1)
				tokenDepth = depthFromSingleToken(token);

			if (namesContainsStar && depthInNames != -1 && tokenDepth != -1) {
				if (depthInNames < tokenDepth)
					return false;
			} else {
				if (other.names.contains(token) == false)
					return false;
			}
		}

		return true;
	}

	private static int depthFromSingleToken(String token) {
		final String prefix = "depth(";
		final int prefixLen = prefix.length();
		if (token.length() > prefixLen + 1 && token.startsWith(prefix) && token.charAt(token.length() - 1) == ')') {
			try {
				return Integer.parseInt(token.substring(prefixLen, token.length() - 1));
			} catch (NumberFormatException e) {
			}
		}
		return -1;
	}

	public final Set<String> getNames() {
		return Collections.unmodifiableSet(names);
	}

	public static StyleSignatureBasic of(SName... names) {
		final List<String> result = new ArrayList<>();
		for (SName name : names)
			result.add(name.name().toLowerCase().replace("_", ""));

		return new StyleSignatureBasic(false, result);
	}

	public StyleSignature forStereotypeItself(Stereotype stereotype) {
		if (stereotype == null || stereotype.getStyleNames().size() == 0)
			return this;

		final StyleSignatures result = new StyleSignatures();
		for (String name : stereotype.getStyleNames()) {
			final List<String> tmp = new ArrayList<>(names);
			tmp.add(SName.stereotype.name().toLowerCase().replace("_", ""));
			tmp.add(clean(name));
			result.add(new StyleSignatureBasic(false, tmp));
		}
		return result;

	}

	@Override
	public StyleSignature withTOBECHANGED(Stereotype stereotype) {
		if (stereotype == null || stereotype.getStyleNames().size() == 0)
			return this;

		final StyleSignatures result = new StyleSignatures();
		for (String name : stereotype.getStyleNames()) {
			final List<String> tmp = new ArrayList<>(names);
			tmp.add(clean(name));
			result.add(new StyleSignatureBasic(true, tmp));
		}
		return result;
	}

	@Override
	public StyleSignature with(Stereostyles stereostyles) {
		if (stereostyles.isEmpty())
			return this;
		final List<String> result = new ArrayList<>(names);
		for (String name : stereostyles.getStyleNames())
			result.add(StereotypeDecoration.PREFIX + clean(name));

		return new StyleSignatureBasic(true, result);
	}

	private String clean(String name) {
		if (name.startsWith("."))
			name = StereotypeDecoration.PREFIX + name;

		return name.toLowerCase().replace("_", "").replace(".", "");
	}

	public StyleSignatureBasic mergeWith(List<Style> others) {
		final List<String> copy = new ArrayList<>(names);
		for (Style other : others)
			for (String s : other.getSignature().getNames())
				copy.add(s);

		return new StyleSignatureBasic(withDot, copy);
	}

	public StyleSignatureBasic mergeWith(StyleSignatureBasic other) {
		final List<String> copy = new ArrayList<>(names);
		copy.addAll(other.names);
		return new StyleSignatureBasic(withDot || other.withDot, copy);
	}

	@Override
	public Style getMergedStyle(StyleBuilder styleBuilder) {
		if (styleBuilder == null)
			return null;

		return styleBuilder.getMergedStyle(this);
	}

	public boolean match(Stereotype stereotype) {
		for (String s : stereotype.getMultipleLabels())
			if (names.contains(clean(s)))
				return true;

		return false;
	}

	public final boolean isWithDot() {
		return withDot;
	}

	// Frequent use

	public static StyleSignatureBasic activity() {
		return StyleSignatureBasic.of(SName.root, SName.element, SName.activityDiagram, SName.activity);
	}

	public static StyleSignatureBasic activityDiamond() {
		return StyleSignatureBasic.of(SName.root, SName.element, SName.activityDiagram, SName.activity, SName.diamond);
	}

	public static StyleSignatureBasic activityArrow() {
		return StyleSignatureBasic.of(SName.root, SName.element, SName.activityDiagram, SName.activity, SName.arrow);
	}

	// Really sorry about that :-)
	public static StyleSignatureBasic of(SName name0, SName name1, SName name2, SName name3, SName[] names) {
		if (names.length == 1)
			return of(name0, name1, name2, name3, names[0]);
		if (names.length == 2)
			return of(name0, name1, name2, name3, names[0], names[1]);
		throw new UnsupportedOperationException();
	}

	public static StyleSignatureBasic of(SName name0, SName name1, SName name2, SName[] sNames, SName... other) {
		final SName[] concat = new SName[3 + sNames.length + other.length];

		concat[0] = name0;
		concat[1] = name1;
		concat[2] = name2;
		System.arraycopy(sNames, 0, concat, 3, sNames.length);
		System.arraycopy(other, 0, concat, 3 + sNames.length, other.length);

		return of(concat);
	}

}