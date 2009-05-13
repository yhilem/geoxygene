/*
 * This file is part of the GeOxygene project source files.
 * 
 * GeOxygene aims at providing an open framework which implements OGC/ISO specifications for
 * the development and deployment of geographic (GIS) applications. It is a open source
 * contribution of the COGIT laboratory at the Institut G�ographique National (the French
 * National Mapping Agency).
 * 
 * See: http://oxygene-project.sourceforge.net
 * 
 * Copyright (C) 2005 Institut G�ographique National
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

package fr.ign.cogit.geoxygene.util.browser;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Cette classe fournit l'impl�mentation de l'�couteur d'�v�nement pour les objets cliquables de type attribut ou mebre d'un attribut (objet ou collection).
 * <br/>Elle permet la navigation g�n�rique entre les objets Java d'une application, en faisant explicitement appel � un nouveau navigateur d'objet
 * graphique pour repr�sent� graphiquement l'�l�ment cliqu�.
 *
 *
 * @author Thierry Badard & Arnaud Braun
 * @version 1.0
 * 
 * 
 */

public class ObjectBrowserAttributeListener implements ActionListener {

	/** Objet portant l'attribut sur lequel on d�finit l'�couteur d'�v�nement ObjectBrowserAttributeListener. */
	private Object obj;

	/**
	 * Constructeur principal de ObjectBrowserAttributeListener.
	 * 
	 * @param obj l'objet portant l'attribut sur lequel on d�finit l'�couteur d'�v�nement ObjectBrowserAttributeListener.
	 */
	public ObjectBrowserAttributeListener(Object obj) {
		this.obj = obj;
	}

	public void actionPerformed(ActionEvent e) {
		ObjectBrowser.browse(this.obj);
	}
}