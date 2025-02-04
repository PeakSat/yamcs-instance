import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { attachContextGuardFn } from '../core/guards/AttachContextGuard';
import { authGuardChildFn, authGuardFn } from '../core/guards/AuthGuard';
import { InstancePage } from '../shared/template/InstancePage';
import { DisplayFilePage } from './displays/DisplayFilePage';
import { DisplayFilePageDirtyGuard, displayFilePageDirtyGuardFn } from './displays/DisplayFilePageDirtyGuard';
import { DisplayFolderPage } from './displays/DisplayFolderPage';
import { DisplayPage } from './displays/DisplayPage';
import { DisplaysPage } from './displays/DisplaysPage';
import { PacketPage } from './packets/PacketPage';
import { PacketsPage } from './packets/PacketsPage';
import { CreateParameterListPage } from './parameter-lists/CreateParameterListPage';
import { EditParameterListPage } from './parameter-lists/EditParameterListPage';
import { ParameterListHistoricalDataTab } from './parameter-lists/ParameterListHistoricalDataTab';
import { ParameterListPage } from './parameter-lists/ParameterListPage';
import { ParameterListSummaryTab } from './parameter-lists/ParameterListSummaryTab';
import { ParameterListsPage } from './parameter-lists/ParameterListsPage';
import { ParameterAlarmsTab } from './parameters/ParameterAlarmsTab';
import { ParameterChartTab } from './parameters/ParameterChartTab';
import { ParameterDataTab } from './parameters/ParameterDataTab';
import { ParameterPage } from './parameters/ParameterPage';
import { ParameterSummaryTab } from './parameters/ParameterSummaryTab';
import { ParametersPage } from './parameters/ParametersPage';

const routes: Routes = [
  {
    path: '',
    canActivate: [authGuardFn, attachContextGuardFn],
    canActivateChild: [authGuardChildFn],
    runGuardsAndResolvers: 'always',
    component: InstancePage,
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'home',
      }, {
        path: 'displays',
        pathMatch: 'full',
        redirectTo: 'displays/browse'
      }, {
        path: 'displays/browse',
        component: DisplaysPage,
        children: [
          {
            path: '**',
            component: DisplayFolderPage,
          }
        ]
      }, {
        path: 'displays/files',
        component: DisplayPage,
        children: [
          {
            path: '**',
            component: DisplayFilePage,
            canDeactivate: [displayFilePageDirtyGuardFn],
          }
        ]
      }, {
        path: 'packets',
        children: [
          {
            path: '',
            pathMatch: 'full',
            component: PacketsPage,
          }, {
            path: ':pname/:gentime/:seqno',
            component: PacketPage,
          }
        ]
      }, {
        path: 'parameters',
        pathMatch: 'full',
        component: ParametersPage,
      }, {
        path: 'parameters/:qualifiedName',
        component: ParameterPage,
        children: [
          {
            path: '',
            pathMatch: 'full',
            redirectTo: 'summary'
          }, {
            path: 'summary',
            component: ParameterSummaryTab,
          }, {
            path: 'chart',
            component: ParameterChartTab,
          }, {
            path: 'data',
            component: ParameterDataTab,
          }, {
            path: 'alarms',
            component: ParameterAlarmsTab,
          }
        ]
      }, {
        path: 'parameter-lists',
        pathMatch: 'full',
        component: ParameterListsPage,
      }, {
        path: 'parameter-lists/create',
        pathMatch: 'full',
        component: CreateParameterListPage,
      }, {
        path: 'parameter-lists/:list',
        component: ParameterListPage,
        children: [
          {
            path: '',
            pathMatch: 'full',
            redirectTo: 'realtime',
          }, {
            path: 'realtime',
            component: ParameterListSummaryTab,
          }, {
            path: 'data',
            component: ParameterListHistoricalDataTab,
          }
        ],
      }, {
        path: 'parameter-lists/:list/edit',
        pathMatch: 'full',
        component: EditParameterListPage,
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    DisplayFilePageDirtyGuard,
  ]
})
export class TelemetryRoutingModule { }

export const routingComponents = [
  CreateParameterListPage,
  DisplayFilePage,
  DisplayFolderPage,
  DisplayPage,
  DisplaysPage,
  EditParameterListPage,
  PacketPage,
  PacketsPage,
  ParameterAlarmsTab,
  ParameterChartTab,
  ParameterDataTab,
  ParameterListHistoricalDataTab,
  ParameterListPage,
  ParameterListsPage,
  ParameterListSummaryTab,
  ParameterPage,
  ParametersPage,
  ParameterSummaryTab,
];
