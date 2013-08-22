package eu.trentorise.smartcampus.service.trento.places;

import it.sayservice.platform.servicebus.test.DataFlowTestHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import smartcampus.service.trento.places.data.message.Trentoplaces.Place;
import smartcampus.service.trento.places.impl.GetComuneStruttureRicettiveDataFlow;
import smartcampus.service.trento.places.impl.GetComuneTrentoPubbliciEserciziDataFlow;

public class TestDataFlow extends TestCase {

	public void test() {
		try {

			Map<String, Object> pars = new HashMap<String, Object>();

			DataFlowTestHelper helper = new DataFlowTestHelper();
			Map<String, Object> out1 = helper.executeDataFlow("smartcampus.service.trento.places", "GetComuneTrentoPubbliciEsercizi", new GetComuneTrentoPubbliciEserciziDataFlow(), pars);
//			Map<String, Object> out1 = helper.executeDataFlow("smartcampus.service.trento.places", "GetComuneStruttureRicettive", new GetComuneStruttureRicettiveDataFlow(), pars);
			for (Place place : (List<Place>) out1.get("data")) {
				// System.out.println(place.getPoi().getPoiId());
				// System.out.println(place.getPoi().getPoiId() + " = " +
				// place.getCategory());
				System.out.println(place.getName() + " = " + place.getCategoriesList());
			}
			System.out.println(((List<Place>) out1.get("data")).size());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
