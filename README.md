# Search Engine
final work from Skillbox (Java)

____
_Java v17.0.5_

_Stack_:
Spring Boot,
JPA,
JSOUP,
SQL,
Morphology Library Lucene.
____

application.yaml
```yaml
indexing-settings:
  sites:
    - url: https://shm.ru
      name: Исторический музей
    - url: https://www.svetlovka.ru
      name: СВЕТЛОВКА
    - url: https://snaplistings.com
      name: Snap listings
```
____
Statistics method returns __info about indexed sites__ to the dashboard.
![image](https://user-images.githubusercontent.com/42184326/178167379-967a8cd2-544f-4bb9-bd5b-0b070b3a090d.png)
____
Management section used to __start/stop indexing__ or to __index/reindex specific webpage or all webpages__, but page have to be related to sites given in application.yml.
![image](https://user-images.githubusercontent.com/42184326/178167559-6789add3-1902-4f28-8a38-b81126cf5683.png)
____
__The search results__ look like this:
![image](https://user-images.githubusercontent.com/42184326/178167659-392fe2af-29c4-4bbe-af1e-885cfdfb7729.png)
____
The search results may differ from the sample.