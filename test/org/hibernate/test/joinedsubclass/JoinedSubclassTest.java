//$Id$
package org.hibernate.test.joinedsubclass;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Property;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class JoinedSubclassTest extends FunctionalTestCase {

	public JoinedSubclassTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "joinedsubclass/Person.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( JoinedSubclassTest.class );
	}

	public void testJoinedSubclass() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Employee mark = new Employee();
		mark.setName("Mark");
		mark.setTitle("internal sales");
		mark.setSex('M');
		mark.setAddress("buckhead");
		mark.setZip("30305");
		mark.setCountry("USA");

		Customer joe = new Customer();
		joe.setName("Joe");
		joe.setAddress("San Francisco");
		joe.setZip("XXXXX");
		joe.setCountry("USA");
		joe.setComments("Very demanding");
		joe.setSex('M');
		joe.setSalesperson(mark);

		Person yomomma = new Person();
		yomomma.setName("mum");
		yomomma.setSex('F');

		s.save(yomomma);
		s.save(mark);
		s.save(joe);

		assertEquals( s.createQuery("from java.io.Serializable").list().size(), 0 );

		assertEquals( s.createQuery("from Person").list().size(), 3 );
		assertEquals( s.createQuery("from Person p where p.class = Customer").list().size(), 1 );
		assertEquals( s.createQuery("from Person p where p.class = Person").list().size(), 1 );
		s.clear();

		List customers = s.createQuery("from Customer c left join fetch c.salesperson").list();
		for ( Iterator iter = customers.iterator(); iter.hasNext(); ) {
			Customer c = (Customer) iter.next();
			assertTrue( Hibernate.isInitialized( c.getSalesperson() ) );
			assertEquals( c.getSalesperson().getName(), "Mark" );
		}
		assertEquals( customers.size(), 1 );
		s.clear();

		customers = s.createQuery("from Customer").list();
		for ( Iterator iter = customers.iterator(); iter.hasNext(); ) {
			Customer c = (Customer) iter.next();
			assertFalse( Hibernate.isInitialized( c.getSalesperson() ) );
			assertEquals( c.getSalesperson().getName(), "Mark" );
		}
		assertEquals( customers.size(), 1 );
		s.clear();


		mark = (Employee) s.get( Employee.class, new Long( mark.getId() ) );
		joe = (Customer) s.get( Customer.class, new Long( joe.getId() ) );

 		mark.setZip("30306");
		assertEquals( s.createQuery("from Person p where p.address.zip = '30306'").list().size(), 1 );

		if ( supportsRowValueConstructorSyntaxInInList() ) {
			s.createCriteria(Person.class).add(
					Expression.in("address", new Address[] { mark.getAddress(), joe.getAddress() } )
			).list();
		}

		s.delete(mark);
		s.delete(joe);
		s.delete(yomomma);
		assertTrue( s.createQuery("from Person").list().isEmpty() );
		t.commit();
		s.close();
	}

	public void testAccessAsIncorrectSubclass() {
		Session s = openSession();
		s.beginTransaction();
		Employee e = new Employee();
		e.setName( "Steve" );
		e.setSex( 'M' );
		e.setTitle( "grand poobah" );
		s.save( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		Customer c = ( Customer ) s.get( Customer.class, new Long( e.getId() ) );
		s.getTransaction().commit();
		s.close();
		assertNull( c );

		s = openSession();
		s.beginTransaction();
		e = ( Employee ) s.get( Employee.class, new Long( e.getId() ) );
		c = ( Customer ) s.get( Customer.class, new Long( e.getId() ) );
		s.getTransaction().commit();
		s.close();
		assertNotNull( e );
		assertNull( c );

		s = openSession();
		s.beginTransaction();
		s.delete( e );
		s.getTransaction().commit();
		s.close();
	}

	public void testQuerySubclassAttribute() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Person p = new Person();
		p.setName("Emmanuel");
		p.setSex('M');
		s.persist(p);
		Employee q = new Employee();
		q.setName("Steve");
		q.setSex('M');
		q.setTitle("Mr");
		q.setSalary( new BigDecimal(1000) );
		s.persist(q);

		List result = s.createQuery("from Person where salary > 100").list();
		assertEquals( result.size(), 1 );
		assertSame( result.get(0), q );

		result = s.createQuery("from Person where salary > 100 or name like 'E%'").list();
		assertEquals( result.size(), 2 );

		result = s.createCriteria(Person.class)
			.add( Property.forName("salary").gt( new BigDecimal(100) ) )
			.list();
		assertEquals( result.size(), 1 );
		assertSame( result.get(0), q );

		//TODO: make this work:
		/*result = s.createQuery("select salary from Person where salary > 100").list();
		assertEquals( result.size(), 1 );
		assertEquals( result.get(0), new BigDecimal(1000) );*/

		s.delete(p);
		s.delete(q);
		t.commit();
		s.close();
	}

	public void testLockingJoinedSubclass() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Person p = new Person();
		p.setName("Emmanuel");
		p.setSex('M');
		s.persist(p);
		Employee q = new Employee();
		q.setName("Steve");
		q.setSex('M');
		q.setTitle("Mr");
		q.setSalary( new BigDecimal(1000) );
		s.persist(q);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.lock( p, LockMode.UPGRADE );
		s.lock( q, LockMode.UPGRADE );
		s.delete( p );
		s.delete( q );
		t.commit();
		s.close();

	}

}

