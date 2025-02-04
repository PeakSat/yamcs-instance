import { SelectionModel } from '@angular/cdk/collections';
import { ChangeDetectionStrategy, Component, ElementRef, OnDestroy, ViewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatTableDataSource } from '@angular/material/table';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { BasenamePipe, ConfigService, ListObjectsOptions, ListObjectsResponse, MessageService, StorageClient, YamcsService } from '@yamcs/webapp-sdk';
import { BehaviorSubject, Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../core/services/AuthService';
import * as dnd from '../../shared/dnd';
import { CreateFolderDialog } from './CreateFolderDialog';
import { CreateStackDialog } from './CreateStackDialog';
import { RenameStackDialog } from './RenameStackDialog';
import { StackFilePage } from './StackFilePage';


@Component({
  templateUrl: './StackFolderPage.html',
  styleUrls: ['./StackFolderPage.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StackFolderPage implements OnDestroy {
  @ViewChild('droparea', { static: true })
  dropArea: ElementRef;

  @ViewChild('uploader')
  private uploaderEl: ElementRef<HTMLInputElement>;

  breadcrumb$ = new BehaviorSubject<BreadCrumbItem[]>([]);
  dragActive$ = new BehaviorSubject<boolean>(false);

  displayedColumns = ['select', 'name', 'modified', 'actions', 'formatWarning'];
  dataSource = new MatTableDataSource<BrowseItem>([]);
  selection = new SelectionModel<BrowseItem>(true, []);

  private routerSubscription: Subscription;
  private storageClient: StorageClient;

  private bucket: string;
  private folderPerInstance: boolean;

  loaded = false;
  converting = false;

  constructor(
    private dialog: MatDialog,
    readonly yamcs: YamcsService,
    title: Title,
    private router: Router,
    private route: ActivatedRoute,
    private authService: AuthService,
    private messageService: MessageService,
    private configService: ConfigService,
    private basenamePipe: BasenamePipe
  ) {
    title.setTitle('Command stacks');
    this.storageClient = yamcs.createStorageClient();

    const config = configService.getConfig();
    this.bucket = configService.getStackBucket();
    this.folderPerInstance = config.stackFolderPerInstance;

    this.loadCurrentFolder();
    this.routerSubscription = router.events.pipe(
      filter(evt => evt instanceof NavigationEnd)
    ).subscribe(() => {
      this.loadCurrentFolder();
    });
  }

  private loadCurrentFolder() {
    const options: ListObjectsOptions = {
      delimiter: '/',
    };

    let prefix = '';
    if (this.folderPerInstance) {
      prefix = this.yamcs.instance! + '/';
    }

    const routeSegments = this.route.snapshot.url;
    if (routeSegments.length) {
      options.prefix = prefix + routeSegments.map(s => s.path).join('/') + '/';
    } else if (prefix) {
      options.prefix = prefix;
    }

    this.storageClient.listObjects(this.bucket, options).then(dir => {
      this.updateBrowsePath();
      this.changedir(dir);
      this.loaded = true;
    });
  }

  private getNameWithoutInstance(name: string) {
    if (this.folderPerInstance) {
      const instance = this.yamcs.instance!;
      return name.substr(instance.length);
    } else {
      return name;
    }
  }

  private changedir(dir: ListObjectsResponse) {
    this.selection.clear();
    const items: BrowseItem[] = [];
    for (const prefix of dir.prefixes || []) {
      items.push({
        folder: true,
        name: prefix,
        nameWithoutInstance: this.getNameWithoutInstance(prefix),
      });
    }
    for (const object of dir.objects || []) {
      // Ignore fake objects that represent an empty directory
      if (object.name.endsWith('/')) {
        continue;
      }
      items.push({
        folder: false,
        name: object.name,
        nameWithoutInstance: this.getNameWithoutInstance(object.name),
        modified: object.created,
        objectUrl: this.storageClient.getObjectURL(this.bucket, object.name),
      });
    }
    this.dataSource.data = items;
  }

  isAllSelected() {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.filteredData.length;
    return numSelected === numRows && numRows > 0;
  }

  masterToggle() {
    this.isAllSelected() ?
      this.selection.clear() :
      this.dataSource.filteredData.forEach(row => this.selection.select(row));
  }

  toggleOne(row: BrowseItem) {
    if (!this.selection.isSelected(row) || this.selection.selected.length > 1) {
      this.selection.clear();
    }
    this.selection.toggle(row);
  }

  createStack() {
    const dialogRef = this.dialog.open(CreateStackDialog, {
      width: '400px',
      data: {
        path: this.getCurrentPath(),
        prefix: this.folderPerInstance ? (this.yamcs.instance! + '/') : '',
      }
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.router.navigateByUrl(`/commanding/stacks/files/${result}?c=${this.yamcs.context}`);
      }
    });
  }

  createFolder() {
    this.dialog.open(CreateFolderDialog, {
      width: '400px',
      data: {
        bucket: this.bucket,
        path: this.getCurrentPath(),
      }
    }).afterClosed().subscribe({
      next: () => this.loadCurrentFolder(),
    });
  }

  openUploadDialog() {
    this.uploaderEl.nativeElement.click();
  }

  importStack() {
    let path = this.getCurrentPath();
    // Full path should not have a leading slash
    if (path.startsWith('/')) {
      path = path.substring(1);
    }

    const files = this.uploaderEl.nativeElement.files;

    const uploadPromises = [];
    for (const key in files) {
      if (!isNaN(parseInt(key, 10))) {
        const file = files[key as any];
        const fullPath = path ? path + '/' + file.name : file.name;
        const prefix = this.folderPerInstance ? (this.yamcs.instance! + '/') : '';
        const objectName = prefix + fullPath;
        const promise = this.storageClient.uploadObject(this.bucket, objectName, file);
        uploadPromises.push(promise);
      }
    }

    Promise.all(uploadPromises)
      .then(() => this.loadCurrentFolder())
      .catch(err => this.messageService.showError(err));
  }

  convertToJSON(event: MouseEvent, name: string) {
    if (this.converting) {
      return;
    }
    if (event.shiftKey || confirm(`Are you sure you want to convert '${name}' to the new format?\nThis wil delete the original XML file.\n(Press shift if you do not want to show this dialog)`)) {
      this.converting = true;
      const response = this.storageClient.getObject(this.bucket, name).then(async response => {
        if (response.ok) {
          const xmlParser = new DOMParser();
          const doc = xmlParser.parseFromString(await response.text(), 'text/xml') as XMLDocument;
          const entries = StackFilePage.parseXML(doc.documentElement, this.configService.getCommandOptions());
          StackFilePage.convertToJSON(this.messageService, this.basenamePipe, this.storageClient, this.bucket, name, entries, {})
            .then(() => {
              this.loadCurrentFolder();
            });
        } else {
          this.messageService.showError("Failed to load '" + name + "' for conversion");
        }
      }).finally(() => { this.converting = false; });

    }
  }

  private getCurrentPath() {
    let path = '';
    for (const segment of this.route.snapshot.url) {
      path += '/' + segment.path;
    }
    return path || '/';
  }

  deleteSelectedStacks() {
    const deletableObjects: string[] = [];
    const findObjectPromises = [];
    for (const item of this.selection.selected) {
      if (item.folder) {
        findObjectPromises.push(this.storageClient.listObjects(this.bucket, {
          prefix: item.name,
        }).then(response => {
          const objects = response.objects || [];
          deletableObjects.push(...objects.map(o => o.name));
        }));
      } else {
        deletableObjects.push(item.name);
      }
    }

    Promise.all(findObjectPromises).then(() => {
      if (confirm(`You are about to delete ${deletableObjects.length} files. Are you sure you want to continue?`)) {
        const deletePromises = [];
        for (const object of deletableObjects) {
          deletePromises.push(this.storageClient.deleteObject(this.bucket, object));
        }

        Promise.all(deletePromises).then(() => {
          this.loadCurrentFolder();
        });
      }
    });
  }

  renameFile(item: BrowseItem) {
    const dialogRef = this.dialog.open(RenameStackDialog, {
      data: {
        name: item.name,
      },
      width: '400px',
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadCurrentFolder();
      }
    });
  }

  deleteFile(item: BrowseItem) {
    if (confirm(`Are you sure you want to delete ${item.nameWithoutInstance}?`)) {
      this.storageClient.deleteObject(this.bucket, item.name).then(() => {
        this.loadCurrentFolder();
      });
    }
  }

  dragEnter(evt: DragEvent) {
    this.dragActive$.next(true);
    evt.preventDefault();
    evt.stopPropagation();
    return false;
  }

  dragOver(evt: DragEvent) { // This event must be prevented. Otherwise drop doesn't trigger.
    evt.preventDefault();
    evt.stopPropagation();
    return false;
  }

  dragLeave(evt: DragEvent) {
    this.dragActive$.next(false);
    evt.preventDefault();
    evt.stopPropagation();
    return false;
  }

  drop(evt: DragEvent) {
    const dataTransfer: any = evt.dataTransfer || {};
    if (dataTransfer) {
      let objectPrefix = this.getCurrentPath().substring(1);
      if (objectPrefix !== '') {
        objectPrefix += '/';
      }

      dnd.listDroppedFiles(dataTransfer).then(droppedFiles => {
        const uploadPromises: any[] = [];
        for (const droppedFile of droppedFiles) {
          let objectPath = objectPrefix + droppedFile._fullPath;
          if (this.folderPerInstance) {
            objectPath = this.yamcs.instance! + '/' + objectPath;
          }
          const promise = this.storageClient.uploadObject(this.bucket, objectPath, droppedFile);
          uploadPromises.push(promise);
        }
        Promise.all(uploadPromises).finally(() => {
          this.loadCurrentFolder();
        });
      });
    }
    this.dragActive$.next(false);
    evt.preventDefault();
    evt.stopPropagation();
    return false;
  }

  mayManageStacks() {
    const user = this.authService.getUser()!;
    return user.hasObjectPrivilege('ManageBucket', this.bucket)
      || user.hasSystemPrivilege('ManageAnyBucket');
  }

  private updateBrowsePath() {
    const breadcrumb: BreadCrumbItem[] = [];
    let path = '';
    for (const segment of this.route.snapshot.url) {
      path += '/' + segment.path;
      breadcrumb.push({
        name: segment.path,
        route: '/commanding/stacks/browse' + path,
      });
    }
    this.breadcrumb$.next(breadcrumb);
    return path || '/';
  }

  ngOnDestroy() {
    this.routerSubscription?.unsubscribe();
  }
}

export class BrowseItem {
  folder: boolean;
  name: string;
  nameWithoutInstance: string;
  modified?: string;
  objectUrl?: string;
}

export interface BreadCrumbItem {
  name: string;
  route: string;
}
