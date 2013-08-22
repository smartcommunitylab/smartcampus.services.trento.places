package eu.trentorise.smartcampus.service.trento.places;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import smartcampus.service.trento.places.data.message.Trentoplaces.Place;

public class ComuneKMLConverter {

	private final static String ESERCIZI_PUBBLICI = "http://webapps.comune.trento.it/cartografia/catalogo?db=base&sc=commercio&ly=civici_pubblici_esercizi&fr=kml";
	private final static String STRUTTURE_RICETTIVE = "http://webapps.comune.trento.it/cartografia/catalogo?db=base&sc=commercio&ly=civici_strutture_ricettive&fr=kml";

	private static Logger log = Logger.getLogger(ComuneKMLConverter.class);

	public static List<Place> readEserciziPubblici() throws Exception {
		return generatePubbliciEsercizi();
	}

	public static List<Place> readStruttureRicettive() throws Exception {
		return generateStruttureRicettive();
	}

	private static List<Place> generatePubbliciEsercizi() throws Exception {
		File f = download(ESERCIZI_PUBBLICI);

		ZipFile zf = new ZipFile(f);

		Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zf.entries();
		List<Place> places = null;
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			places = parseXMLPubbliciEsercizi(zf.getInputStream(entry));
		}

		return places;
	}

	private static List<Place> generateStruttureRicettive() throws Exception {
		File f = download(STRUTTURE_RICETTIVE);

		ZipFile zf = new ZipFile(f);

		Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zf.entries();
		List<Place> places = null;
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			places = parseXMLStruttureRicettive(zf.getInputStream(entry));
		}

		return places;
	}

	private static File download(String address) throws Exception {
		String tmp = System.getProperty("java.io.tmpdir");
		File f = new File(tmp, "tmpzip.zip");
		// FileUtils.copyURLToFile(new URL(address), f);

		FileOutputStream fos = new FileOutputStream(f);

		BufferedInputStream in = new BufferedInputStream(new URL(address).openStream());
		byte data[] = new byte[1024];
		int count;
		while ((count = in.read(data, 0, 1024)) != -1) {
			fos.write(data, 0, count);
		}
		fos.close();

		return f;
	}

	private static List<Place> parseXMLPubbliciEsercizi(InputStream is) throws Exception {
		List<Place> result = new ArrayList<Place>();

		Map<String, Place.Builder> map = new TreeMap<String, Place.Builder>();

		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);

		NodeList nodes = (NodeList) XPathFactory.newInstance().newXPath().compile("//Placemark").evaluate(doc.getDocumentElement(), XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			Element element = (Element) nodes.item(i);
			Place.Builder builder = Place.newBuilder();

			NodeList attributes = (NodeList) XPathFactory.newInstance().newXPath().compile("ExtendedData/SchemaData/SimpleData").evaluate(element, XPathConstants.NODESET);
			for (int j = 0; j < attributes.getLength(); j++) {
				String attr = ((Element) attributes.item(j)).getAttribute("name");
				String value = ((Element) attributes.item(j)).getTextContent();
				if ("peinse".equals(attr)) {
					String name = value.trim();
					builder.setName(name);
				} else if ("desvia".equals(attr)) {
					builder.setStreet(value.trim());
				} else if ("civico_alf".equals(attr)) {
					builder.setNumber(value.trim());
				} else if ("tgdesc31".equals(attr)) {
					builder.addCategories(value.trim());
				}
			}

			if (map.containsKey(builder.getName())) {
				Place.Builder old = map.get(builder.getName());
				builder.addAllCategories(old.getCategoriesList());
			}
			map.put(builder.getName(), builder);

			attributes = (NodeList) XPathFactory.newInstance().newXPath().compile("Point/coordinates").evaluate(element, XPathConstants.NODESET);
			for (int j = 0; j < attributes.getLength(); j++) {
				String attr = ((Element) attributes.item(j)).getAttribute("name");
				String value = ((Element) attributes.item(j)).getTextContent();
				String lonlat[] = value.split(",");
				// swapped
				builder.setLongitude(Double.parseDouble(lonlat[0]));
				builder.setLatitude(Double.parseDouble(lonlat[1]));
			}

			builder.setTown("Trento");
			builder.setProvince("TN");
			builder.setSource("Open Data - Pubblici esercizi");
		}
		
		for (Place.Builder builder : map.values()) {
			String oldTags = builder.getCategoriesList().toString();
			List<String> newTags = new ArrayList<String>();
			if (oldTags.contains("Pizze")) {
				newTags.add("Pizzeria");
			} else 	if (oldTags.contains("tradizionali")) {
				newTags.add("Restaurant");
			}
			if ((oldTags.contains("Bevande") || oldTags.contains("veloci")) && newTags.size() == 0) {
				newTags.add("Bar");
			}
			builder.clearCategories();
			builder.addAllCategories(newTags);
			result.add(builder.build());
		}

		return result;
	}

	private static List<Place> parseXMLStruttureRicettive(InputStream is) throws Exception {
		List<Place> result = new ArrayList<Place>();

		Map<String, Place.Builder> map = new TreeMap<String, Place.Builder>();

		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);

		NodeList nodes = (NodeList) XPathFactory.newInstance().newXPath().compile("//Placemark").evaluate(doc.getDocumentElement(), XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			Element element = (Element) nodes.item(i);
			Place.Builder builder = Place.newBuilder();

			NodeList attributes = (NodeList) XPathFactory.newInstance().newXPath().compile("ExtendedData/SchemaData/SimpleData").evaluate(element, XPathConstants.NODESET);
			for (int j = 0; j < attributes.getLength(); j++) {
				String attr = ((Element) attributes.item(j)).getAttribute("name");
				String value = ((Element) attributes.item(j)).getTextContent();
				if ("peinse".equals(attr)) {
					String name = value.trim();
					builder.setName(name);
				} else if ("desvia".equals(attr)) {
					builder.setStreet(value.trim());
				} else if ("civico_alf".equals(attr)) {
					builder.setNumber(value.trim());
				}
			}

			if (map.containsKey(builder.getName())) {
				Place.Builder old = map.get(builder.getName());
				builder.addAllCategories(old.getCategoriesList());
			}
			map.put(builder.getName(), builder);

			attributes = (NodeList) XPathFactory.newInstance().newXPath().compile("Point/coordinates").evaluate(element, XPathConstants.NODESET);
			for (int j = 0; j < attributes.getLength(); j++) {
				String attr = ((Element) attributes.item(j)).getAttribute("name");
				String value = ((Element) attributes.item(j)).getTextContent();
				String lonlat[] = value.split(",");
				// swapped
				builder.setLongitude(Double.parseDouble(lonlat[0]));
				builder.setLatitude(Double.parseDouble(lonlat[1]));
			}

			builder.setTown("Trento");
			builder.setProvince("TN");
			builder.setSource("Open Data - Strutture ricettive");
		}

		List<String> skip = Arrays.asList(new String[] { "affittacamere", "alloggi", "appartamenti", "casa per", "foresteria" });

		for (Place.Builder builder : map.values()) {
			String name = builder.getName().toLowerCase();
			boolean ok = true;
//			for (String sk : skip) {
//				if (name.contains(sk)) {
//					builder.addCategories("Accommodation");
//					ok = false;
//				}
//			}
			if (ok) {
				if (name.contains("albergo") || name.contains("hotel")) {
					builder.addCategories("Hotel");
				}
				if (name.contains("bed") && name.contains("breakfast")) {
					builder.addCategories("Bed and Breakfast");
				}			
				if (name.contains("ostello")) {
					builder.addCategories("Hostel");
				}				
				if (builder.getCategoriesList().size() == 0) {
					builder.addCategories("Accommodation");
				}
			}
			result.add(builder.build());
		}		

		return result;
	}

}
