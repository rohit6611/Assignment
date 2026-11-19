# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes, I would refactor toward a consistent approach. Currently there are three different strategies:

1. Store uses the Active Record pattern (PanacheEntity) — the entity itself handles persistence
   (Store.findById, store.persist, store.delete).
2. Product uses the Repository pattern (PanacheRepository<Product>) — a separate ProductRepository
   bean handles persistence, but the entity is a plain JPA entity with @Id/@GeneratedValue.
3. Warehouse uses a Hexagonal/Ports-and-Adapters architecture — a domain model (Warehouse),
   a DB entity (DbWarehouse), a port interface (WarehouseStore), and an adapter (WarehouseRepository).

I would standardize on the Repository + Ports-and-Adapters approach (option 3) for the following reasons:

- Separation of concerns: keeping domain models independent from JPA annotations makes the
  business logic testable without a database or container.
- Testability: port interfaces are easy to mock in unit tests, while Active Record patterns
  tie tests to the persistence framework.
- Flexibility: if we ever need to swap the data store or introduce caching, the port interface
  isolates the change to the adapter layer.
- Consistency: a single pattern across all entities reduces cognitive load for the team.

The Active Record pattern (Store) is the first I would refactor, as it tightly couples the entity
to the framework and makes unit testing harder. The Product approach is better but still lacks
the domain/infrastructure separation that the Warehouse design provides.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
API-First (OpenAPI spec + code generation) — used by Warehouse:

Pros:
- Contract-first design enforces that the API is designed before implementation, promoting
  better API design and consistency.
- The spec serves as living documentation that is always in sync with the code.
- Clients can generate SDKs from the spec, enabling parallel frontend/backend development.
- Changes to the API are explicitly versioned in the spec file, making breaking changes visible
  in code review.

Cons:
- Adds build complexity — the OpenAPI generator plugin is an extra dependency and build step.
- Generated code can be hard to customize or debug.
- Developers need to learn the OpenAPI specification format and the generator's conventions.
- Simple changes may require touching both the spec and the implementation class.

Code-First (hand-coded JAX-RS annotations) — used by Product and Store:

Pros:
- Faster to get started, simpler setup, fewer moving parts.
- Full control over the resource class, easier to debug.
- Lower learning curve for developers already familiar with JAX-RS.

Cons:
- The API shape is an implicit side-effect of the code, easy to break unintentionally.
- Documentation must be maintained separately or generated after the fact, often drifting.
- No compile-time contract to validate against.

My choice: I would go with API-First (OpenAPI + code generation) for all endpoints. In a team
environment, having a single source of truth for the API contract outweighs the extra setup
complexity. It improves communication between frontend and backend teams, enables automated
contract testing, and makes the API surface explicit and reviewable. For small utility endpoints,
code-first may be acceptable, but for domain-critical resources like Store and Product, the
discipline of a formal spec pays off quickly.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
I would prioritize tests in the following order:

1. Unit tests for domain use cases (highest priority)
   These are the fastest to write and run, have no infrastructure dependencies, and validate
   the core business logic — validation rules, constraint checks, and state transitions.
   Examples: CreateWarehouseUseCaseTest, ReplaceWarehouseUseCaseTest, ArchiveWarehouseUseCaseTest.
   Using mocks for ports (WarehouseStore, LocationResolver) keeps them isolated and fast.

2. Integration tests for REST endpoints
   These verify the full HTTP request/response cycle including serialization, status codes,
   and error handling. They catch wiring issues (CDI injection, transaction management) that
   unit tests miss. Examples: WarehouseEndpointIT, ProductEndpointTest.
   Quarkus DevServices makes these practical by auto-provisioning a test database.

3. Repository/adapter tests
   Testing the database adapter layer with a real database (via DevServices) ensures queries,
   mappings, and JPA behavior work correctly. These are especially valuable for custom queries
   like findByBusinessUnitCode or findActiveByLocation.

4. Edge case and negative-path tests
   Testing invalid inputs, constraint violations, and concurrent scenarios ensures the system
   fails gracefully.

To ensure coverage remains effective over time:
- Run tests in CI on every pull request — no merge without green tests.
- Track code coverage metrics (JaCoCo) to identify untested paths, but do not chase a coverage
  percentage blindly — focus on testing behavior, not lines.
- Review tests during code review as carefully as production code.
- When fixing a bug, write a regression test that reproduces it before applying the fix.
- Keep tests fast: unit tests should run in milliseconds, integration tests in seconds.
  Slow tests get skipped and eventually ignored.
```
