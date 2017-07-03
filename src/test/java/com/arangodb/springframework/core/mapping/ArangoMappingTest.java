/*
 * DISCLAIMER
 *
 * Copyright 2017 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb.springframework.core.mapping;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.core.ArangoOperations;

/**
 * @author Mark Vollmary
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ArangoTestConfiguration.class })
public class ArangoMappingTest {

	private static final String[] COLLECTIONS = new String[] { "customer", "shopping-cart", "product" };

	@Autowired
	private ArangoOperations template;

	@Before
	public void before() {
		try {
			for (final String collection : COLLECTIONS) {
				template.driver().db(ArangoTestConfiguration.DB).collection(collection).drop();
			}
		} catch (final Exception e) {
		}
		for (final String collection : COLLECTIONS) {
			template.driver().db(ArangoTestConfiguration.DB).createCollection(collection);
		}
	}

	@After
	public void after() {
		try {
			for (final String collection : COLLECTIONS) {
				template.driver().db(ArangoTestConfiguration.DB).collection(collection).drop();
			}
		} catch (final Exception e) {
		}
	}

	@Test
	public void id() {
		final DocumentCreateEntity<ShoppingCart> ref = template.insertDocument(new ShoppingCart());
		final ShoppingCart cart = template.getDocument(ref.getId(), ShoppingCart.class);
		assertThat(cart, is(notNullValue()));
		assertThat(cart.getId(), is(ref.getId()));
	}

	@Test
	public void singleReference() {
		final DocumentCreateEntity<Customer> ref;
		{
			final Customer customer = new Customer();
			customer.setShoppingCart(new ShoppingCart());
			ref = template.insertDocument(customer);
		}
		final Customer customerDocument = template.getDocument(ref.getId(), Customer.class);
		assertThat(customerDocument, is(notNullValue()));
		final ShoppingCart shoppingCart = customerDocument.getShoppingCart();
		assertThat(shoppingCart, is(notNullValue()));
		assertThat(shoppingCart.getId(), is(notNullValue()));

		final ShoppingCart shoppingCartDocument = template.getDocument(shoppingCart.getId(), ShoppingCart.class);
		assertThat(shoppingCartDocument, is(notNullValue()));
		assertThat(shoppingCartDocument.getId(), is(shoppingCart.getId()));
	}

	@Test
	public void referenceList() {
		final DocumentCreateEntity<Customer> ref;
		{
			final Customer customer = new Customer();
			final ShoppingCart shoppingCart = new ShoppingCart();
			final Collection<Product> products = new ArrayList<>();
			products.add(new Product("a"));
			products.add(new Product("b"));
			products.add(new Product("c"));
			shoppingCart.setProducts(products);
			customer.setShoppingCart(shoppingCart);
			ref = template.insertDocument(customer);
		}
		final Customer customerDocument = template.getDocument(ref.getId(), Customer.class);
		final Collection<Product> products = customerDocument.getShoppingCart().getProducts();
		assertThat(products.size(), is(3));
		assertThat(products.stream().map(e -> e.getName()).collect(Collectors.toList()), hasItems("a", "b", "c"));
	}

}
