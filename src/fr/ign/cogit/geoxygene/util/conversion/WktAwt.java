/*
 * This file is part of the GeOxygene project source files.
 * 
 * GeOxygene aims at providing an open framework which implements OGC/ISO specifications for
 * the development and deployment of geographic (GIS) applications. It is a open source
 * contribution of the COGIT laboratory at the Institut Géographique National (the French
 * National Mapping Agency).
 * 
 * See: http://oxygene-project.sourceforge.net
 * 
 * Copyright (C) 2005 Institut Géographique National
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this library (see file LICENSE if present); if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 */

/* Generated By:JavaCC: Do not edit this line. WktAwt.java */
package fr.ign.cogit.geoxygene.util.conversion;

import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.StringTokenizer;

public class WktAwt implements WktAwtConstants {
	static class EndOfFile extends Exception {private static final long serialVersionUID = 1L;}
	static class EmptyLine extends Exception {private static final long serialVersionUID = 1L;}

	/*-----------------------------------------------------*/
	/*- Create AwtShape from Wkt object(s) ----------------*/
	/*-----------------------------------------------------*/

	public static AwtShape makeAwtShape(InputStream in)
	throws ParseException
	{
		WktAwt parser=new WktAwt(in);
		AwtAggregate geom=new AwtAggregate();

		while (true) {
			try {
				geom.add(parser.parseOneLine());
			} catch (EndOfFile e) {
				break;
			} catch (EmptyLine e) {}
		}
		return geom;
	}

	public static AwtShape makeAwtShape(String wkt)
	throws ParseException
	{
		InputStream in=new ByteArrayInputStream(wkt.getBytes());
		return makeAwtShape(in);
	}

	/*-----------------------------------------------------*/
	/*- Main function for testing -------------------------*/
	/*-----------------------------------------------------*/

	static Ellipse2D makeVisiblePoint(Point2D point)
	{
		return new Ellipse2D.Double(point.getX()-5, point.getY()-5, 10, 10);
	}

	final public Point2D point() throws ParseException {
		Token xy;
		xy = jj_consume_token(POINT);
		StringTokenizer tkz=new StringTokenizer(xy.image);
		String xStr=tkz.nextToken();
		String yStr=tkz.nextToken();
		double x=Double.parseDouble(xStr);
		double y=Double.parseDouble(yStr);
		{if (true) return new Point2D.Double(x,y);}
		throw new Error("Missing return statement in function");
	}

	final public Point2D pointText() throws ParseException {
		Point2D p=new Point2D.Double(Double.NaN, Double.NaN);
		switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
		case 9:
			jj_consume_token(9);
			p = point();
			jj_consume_token(10);
			break;
		case 11:
			jj_consume_token(11);
			break;
		default:
			jj_la1[0] = jj_gen;
		jj_consume_token(-1);
		throw new ParseException();
		}
		{if (true) return p;}
		throw new Error("Missing return statement in function");
	}

	final public GeneralPath linestringText() throws ParseException {
		GeneralPath lineString=new GeneralPath();
		Point2D p;
		switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
		case 9:
			jj_consume_token(9);
			p = point();
			lineString.moveTo((float)p.getX(), (float)p.getY());
			label_1:
				while (true) {
					switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
					case 12:
						;
						break;
					default:
						jj_la1[1] = jj_gen;
					break label_1;
					}
					jj_consume_token(12);
					p = point();
					lineString.lineTo((float)p.getX(), (float)p.getY());
				}
			jj_consume_token(10);
			break;
		case 11:
			jj_consume_token(11);
			break;
		default:
			jj_la1[2] = jj_gen;
		jj_consume_token(-1);
		throw new ParseException();
		}
		{if (true) return lineString;}
		throw new Error("Missing return statement in function");
	}

	final public AwtShape polygonText() throws ParseException {
		AwtSurface poly=new AwtSurface();
		GeneralPath lineString;
		switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
		case 9:
			jj_consume_token(9);
			lineString = linestringText();
			lineString.closePath();
			poly=new AwtSurface(lineString);
			label_2:
				while (true) {
					switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
					case 12:
						;
						break;
					default:
						jj_la1[3] = jj_gen;
					break label_2;
					}
					jj_consume_token(12);
					lineString = linestringText();
					lineString.closePath();
					poly.addInterior(lineString);
				}
			jj_consume_token(10);
			break;
		case 11:
			jj_consume_token(11);
			break;
		default:
			jj_la1[4] = jj_gen;
		jj_consume_token(-1);
		throw new ParseException();
		}
		{if (true) return poly;}
		throw new Error("Missing return statement in function");
	}

	final public AwtShape multipointText() throws ParseException {
		GeneralPath multi=new GeneralPath();
		Point2D p;
		switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
		case 9:
			jj_consume_token(9);
			p = point();
			multi.append(new GeneralPath(makeVisiblePoint(p)), false);
			label_3:
				while (true) {
					switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
					case 12:
						;
						break;
					default:
						jj_la1[5] = jj_gen;
					break label_3;
					}
					jj_consume_token(12);
					p = point();
					multi.append(new GeneralPath(makeVisiblePoint(p)), false);
				}
			jj_consume_token(10);
			break;
		case 11:
			jj_consume_token(11);
			break;
		default:
			jj_la1[6] = jj_gen;
		jj_consume_token(-1);
		throw new ParseException();
		}
		{if (true) return new AwtSurface(multi);}
		throw new Error("Missing return statement in function");
	}

	final public AwtShape multilinestringText() throws ParseException {
		GeneralPath multi=new GeneralPath();
		GeneralPath lineString;
		switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
		case 9:
			jj_consume_token(9);
			lineString = linestringText();
			multi.append(lineString,false);
			label_4:
				while (true) {
					switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
					case 12:
						;
						break;
					default:
						jj_la1[7] = jj_gen;
					break label_4;
					}
					jj_consume_token(12);
					lineString = linestringText();
					multi.append(lineString,false);
				}
			jj_consume_token(10);
			break;
		case 11:
			jj_consume_token(11);
			break;
		default:
			jj_la1[8] = jj_gen;
		jj_consume_token(-1);
		throw new ParseException();
		}
		{if (true) return new AwtOutline(multi);}
		throw new Error("Missing return statement in function");
	}

	final public AwtShape multipolygonText() throws ParseException {
		AwtAggregate multi=new AwtAggregate();
		AwtShape poly;
		switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
		case 9:
			jj_consume_token(9);
			poly = polygonText();
			multi.add(poly);
			label_5:
				while (true) {
					switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
					case 12:
						;
						break;
					default:
						jj_la1[9] = jj_gen;
					break label_5;
					}
					jj_consume_token(12);
					poly = polygonText();
					multi.add(poly);
				}
			jj_consume_token(10);
			break;
		case 11:
			jj_consume_token(11);
			break;
		default:
			jj_la1[10] = jj_gen;
		jj_consume_token(-1);
		throw new ParseException();
		}
		{if (true) return multi;}
		throw new Error("Missing return statement in function");
	}

	final public AwtShape geometrycollectionText() throws ParseException {
		AwtAggregate collec=new AwtAggregate();
		AwtShape geom;
		switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
		case 9:
			jj_consume_token(9);
			geom = geometryTaggedText();
			collec.add(geom);
			label_6:
				while (true) {
					switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
					case 12:
						;
						break;
					default:
						jj_la1[11] = jj_gen;
					break label_6;
					}
					jj_consume_token(12);
					geom = geometryTaggedText();
					collec.add(geom);
				}
			jj_consume_token(10);
			break;
		case 11:
			jj_consume_token(11);
			break;
		default:
			jj_la1[12] = jj_gen;
		jj_consume_token(-1);
		throw new ParseException();
		}
		{if (true) return collec;}
		throw new Error("Missing return statement in function");
	}

	final public AwtShape pointTaggedText() throws ParseException {
		Point2D p;
		jj_consume_token(13);
		p = pointText();
		{if (true) return new AwtSurface(makeVisiblePoint(p));}
		throw new Error("Missing return statement in function");
	}

	final public AwtShape linestringTaggedText() throws ParseException {
		GeneralPath lineString;
		jj_consume_token(14);
		lineString = linestringText();
		{if (true) return new AwtOutline(lineString);}
		throw new Error("Missing return statement in function");
	}

	final public AwtShape multipointTaggedText() throws ParseException {
		AwtShape multi;
		jj_consume_token(15);
		multi = multipointText();
		{if (true) return multi;}
		throw new Error("Missing return statement in function");
	}

	final public AwtShape multilinestringTaggedText() throws ParseException {
		AwtShape multi;
		jj_consume_token(16);
		multi = multilinestringText();
		{if (true) return multi;}
		throw new Error("Missing return statement in function");
	}

	final public AwtShape polygonTaggedText() throws ParseException {
		AwtShape poly;
		jj_consume_token(17);
		poly = polygonText();
		{if (true) return poly;}
		throw new Error("Missing return statement in function");
	}

	final public AwtShape multipolygonTaggedText() throws ParseException {
		AwtShape multi;
		jj_consume_token(18);
		multi = multipolygonText();
		{if (true) return multi;}
		throw new Error("Missing return statement in function");
	}

	final public AwtShape geometrycollectionTaggedText() throws ParseException {
		AwtShape collec;
		jj_consume_token(19);
		collec = geometrycollectionText();
		{if (true) return collec;}
		throw new Error("Missing return statement in function");
	}

	final public AwtShape geometryTaggedText() throws ParseException {
		AwtShape geom;
		switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
		case 13:
			geom = pointTaggedText();
			break;
		case 14:
			geom = linestringTaggedText();
			break;
		case 17:
			geom = polygonTaggedText();
			break;
		case 15:
			geom = multipointTaggedText();
			break;
		case 16:
			geom = multilinestringTaggedText();
			break;
		case 18:
			geom = multipolygonTaggedText();
			break;
		case 19:
			geom = geometrycollectionTaggedText();
			break;
		default:
			jj_la1[13] = jj_gen;
		jj_consume_token(-1);
		throw new ParseException();
		}
		{if (true) return geom;}
		throw new Error("Missing return statement in function");
	}

	final public AwtShape parseOneLine() throws ParseException, EmptyLine, EndOfFile {
		AwtShape geom;
		switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
		case 13:
		case 14:
		case 15:
		case 16:
		case 17:
		case 18:
		case 19:
			geom = geometryTaggedText();
			switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
			case 0:
				jj_consume_token(0);
				break;
			case EOL:
				jj_consume_token(EOL);
				break;
			default:
				jj_la1[14] = jj_gen;
			jj_consume_token(-1);
			throw new ParseException();
			}
			{if (true) return geom;}
			break;
		case EOL:
			jj_consume_token(EOL);
			{if (true) throw new EmptyLine();} {if (true) return null;}
			break;
		case 0:
			jj_consume_token(0);
			{if (true) throw new EndOfFile();} {if (true) return null;}
			break;
		default:
			jj_la1[15] = jj_gen;
		jj_consume_token(-1);
		throw new ParseException();
		}
		throw new Error("Missing return statement in function");
	}

	public WktAwtTokenManager token_source;
	SimpleCharStream jj_input_stream;
	public Token token, jj_nt;
	private int jj_ntk;
	private int jj_gen;
	final private int[] jj_la1 = new int[16];
	final private int[] jj_la1_0 = {0xa00,0x1000,0xa00,0x1000,0xa00,0x1000,0xa00,0x1000,0xa00,0x1000,0xa00,0x1000,0xa00,0xfe000,0x41,0xfe041,};

	public WktAwt(java.io.InputStream stream) {
		jj_input_stream = new SimpleCharStream(stream, 1, 1);
		token_source = new WktAwtTokenManager(jj_input_stream);
		token = new Token();
		jj_ntk = -1;
		jj_gen = 0;
		for (int i = 0; i < 16; i++) jj_la1[i] = -1;
	}

	public void ReInit(java.io.InputStream stream) {
		jj_input_stream.ReInit(stream, 1, 1);
		token_source.ReInit(jj_input_stream);
		token = new Token();
		jj_ntk = -1;
		jj_gen = 0;
		for (int i = 0; i < 16; i++) jj_la1[i] = -1;
	}

	public WktAwt(java.io.Reader stream) {
		jj_input_stream = new SimpleCharStream(stream, 1, 1);
		token_source = new WktAwtTokenManager(jj_input_stream);
		token = new Token();
		jj_ntk = -1;
		jj_gen = 0;
		for (int i = 0; i < 16; i++) jj_la1[i] = -1;
	}

	public void ReInit(java.io.Reader stream) {
		jj_input_stream.ReInit(stream, 1, 1);
		token_source.ReInit(jj_input_stream);
		token = new Token();
		jj_ntk = -1;
		jj_gen = 0;
		for (int i = 0; i < 16; i++) jj_la1[i] = -1;
	}

	public WktAwt(WktAwtTokenManager tm) {
		token_source = tm;
		token = new Token();
		jj_ntk = -1;
		jj_gen = 0;
		for (int i = 0; i < 16; i++) jj_la1[i] = -1;
	}

	public void ReInit(WktAwtTokenManager tm) {
		token_source = tm;
		token = new Token();
		jj_ntk = -1;
		jj_gen = 0;
		for (int i = 0; i < 16; i++) jj_la1[i] = -1;
	}

	final private Token jj_consume_token(int kind) throws ParseException {
		Token oldToken;
		if ((oldToken = token).next != null) token = token.next;
		else token = token.next = token_source.getNextToken();
		jj_ntk = -1;
		if (token.kind == kind) {
			jj_gen++;
			return token;
		}
		token = oldToken;
		jj_kind = kind;
		throw generateParseException();
	}

	final public Token getNextToken() {
		if (token.next != null) token = token.next;
		else token = token.next = token_source.getNextToken();
		jj_ntk = -1;
		jj_gen++;
		return token;
	}

	final public Token getToken(int index) {
		Token t = token;
		for (int i = 0; i < index; i++) {
			if (t.next != null) t = t.next;
			else t = t.next = token_source.getNextToken();
		}
		return t;
	}

	final private int jj_ntk() {
		if ((jj_nt=token.next) == null)
			return (jj_ntk = (token.next=token_source.getNextToken()).kind);
		else
			return (jj_ntk = jj_nt.kind);
	}

	private java.util.Vector<Object> jj_expentries = new java.util.Vector<Object>();
	private int[] jj_expentry;
	private int jj_kind = -1;

	final public ParseException generateParseException() {
		jj_expentries.removeAllElements();
		boolean[] la1tokens = new boolean[20];
		for (int i = 0; i < 20; i++) {
			la1tokens[i] = false;
		}
		if (jj_kind >= 0) {
			la1tokens[jj_kind] = true;
			jj_kind = -1;
		}
		for (int i = 0; i < 16; i++) {
			if (jj_la1[i] == jj_gen) {
				for (int j = 0; j < 32; j++) {
					if ((jj_la1_0[i] & (1<<j)) != 0) {
						la1tokens[j] = true;
					}
				}
			}
		}
		for (int i = 0; i < 20; i++) {
			if (la1tokens[i]) {
				jj_expentry = new int[1];
				jj_expentry[0] = i;
				jj_expentries.addElement(jj_expentry);
			}
		}
		int[][] exptokseq = new int[jj_expentries.size()][];
		for (int i = 0; i < jj_expentries.size(); i++) {
			exptokseq[i] = (int[])jj_expentries.elementAt(i);
		}
		return new ParseException(token, exptokseq, tokenImage);
	}

	final public void enable_tracing() {
	}

	final public void disable_tracing() {
	}

}
